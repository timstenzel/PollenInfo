import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

plugins {
    // AGP 9 compiles Kotlin itself; no separate Kotlin Android plugin.
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    // Reads androidApp/google-services.json (committed; it identifies the Firebase project and is
    // not a secret) into resources that firebase-messaging initialises from.
    alias(libs.plugins.googleServices)
    // Uploads the R8 mapping of every release build to Firebase so Crashlytics shows readable stack
    // traces. The library itself is a dependency of :composeApp.
    alias(libs.plugins.firebaseCrashlytics)
}

// Raised by hand when a release counts as a new version (see deploy/README.md, "Cutting a release").
val appVersionName = "1.0.0"

// The number of commits up to HEAD, so every release on main carries a higher version code than the
// one before. A shallow checkout (pull request CI) counts only its own commits and gets 1; without
// git at all the code is 1 too. Read through providers.exec, so it is a configuration-cache input:
// a new commit invalidates the cache entry.
val appVersionCode: Int = try {
    providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
        workingDir = rootDir
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().toIntOrNull()?.takeIf { it > 0 } ?: 1
} catch (_: Exception) {
    1
}

// Release signing from the environment (the CI release job writes the keystore to a temporary file
// and passes its path). Applied only when all four are set; otherwise the release stays unsigned.
// Android Studio's signing wizard injects its own signing properties and works without them.
val releaseSigning: Map<String, String>? = listOf(
    "ANDROID_KEYSTORE_FILE",
    "ANDROID_KEYSTORE_PASSWORD",
    "ANDROID_KEY_ALIAS",
    "ANDROID_KEY_PASSWORD",
).associateWith { providers.environmentVariable(it).orNull.orEmpty() }
    .takeIf { values -> values.values.all { it.isNotEmpty() } }

android {
    namespace = "ch.stenzel.tim.polleninfo"
    compileSdk = 37

    defaultConfig {
        applicationId = "ch.stenzel.tim.polleninfo"
        minSdk = 26
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (releaseSigning != null) {
            create("release") {
                storeFile = file(releaseSigning.getValue("ANDROID_KEYSTORE_FILE"))
                storePassword = releaseSigning.getValue("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = releaseSigning.getValue("ANDROID_KEY_ALIAS")
                keyPassword = releaseSigning.getValue("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            isDebuggable = true
            // The development backend on the host machine (10.0.2.2 is the emulator's alias for its
            // loopback), or any address passed as -Ppolleninfo.apiBaseUrl=… or set in
            // ~/.gradle/gradle.properties — e.g. the deployed backend from a debug build.
            val apiBaseUrl = providers.gradleProperty("polleninfo.apiBaseUrl")
                .getOrElse("http://10.0.2.2:8080")
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // Always the deployed backend over HTTPS; release builds have no cleartext exception.
            buildConfigField("String", "API_BASE_URL", "\"https://api.polleninfo.ch\"")
            if (releaseSigning != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // On by default so no release ships with unreadable crashes. An offline or quick local
            // release passes -Ppolleninfo.uploadCrashlyticsMapping=false and accepts that its
            // crashes stay obfuscated.
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = providers.gradleProperty("polleninfo.uploadCrashlyticsMapping")
                    .map(String::toBoolean)
                    .getOrElse(true)
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    // The Android gate runs :androidApp:lintRelease and fails on any finding, warnings included.
    // checkDependencies analyses :composeApp's Android code and :theme from here. No baseline: a
    // finding is fixed, not recorded. The two version checks are off because they depend on what
    // was released overnight, and dependency updates have their own process (Dependabot).
    lint {
        warningsAsErrors = true
        abortOnError = true
        checkDependencies = true
        disable += setOf("NewerVersionAvailable", "GradleDependency")
    }
}

// AGP 9's built-in Kotlin. Any Kotlin compiler warning fails the build, locally and in CI.
kotlin {
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

// Prints the version the build uses, one `key=value` per line, for the CI release job
// (`./gradlew -q :androidApp:printVersion`).
tasks.register("printVersion") {
    description = "Prints the app's versionName and versionCode."
    val name = appVersionName
    val code = appVersionCode
    doLast {
        println("versionName=$name")
        println("versionCode=$code")
    }
}

dependencies {
    // The app itself: every screen, the platform actuals, the push service and their resources.
    implementation(project(":composeApp"))
    implementation(project(":theme"))

    // Used directly by MainActivity (setContent, AppCompatActivity) and PollenInfoApplication
    // (startKoin with androidContext, the process-lifetime scope of the crash-reporting sync).
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.android)
}
