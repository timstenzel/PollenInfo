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

android {
    namespace = "ch.stenzel.tim.polleninfo"
    compileSdk = 37

    defaultConfig {
        applicationId = "ch.stenzel.tim.polleninfo"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
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
