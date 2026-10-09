plugins {
    // AGP 9 compiles Kotlin itself; no separate Kotlin Android plugin.
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    // Reads androidApp/google-services.json (committed; it identifies the Firebase project and is
    // not a secret) into resources that firebase-messaging initialises from.
    alias(libs.plugins.googleServices)
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

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
    // (startKoin with androidContext).
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.koin.android)
}
