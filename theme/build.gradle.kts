import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    // Gives the KMP library plugin lint tasks for androidMain, so :androidApp:lintRelease
    // (checkDependencies) analyses this module too — without it the module is silently skipped.
    alias(libs.plugins.androidLint)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    // Any Kotlin compiler warning fails the build, locally and in CI, for every target.
    compilerOptions {
        allWarningsAsErrors.set(true)
    }

    android {
        namespace = "ch.stenzel.tim.polleninfo.theme"
        compileSdk = 37
        minSdk = 26

        // Compose resources (the fonts) are packaged as Android assets only with this on.
        androidResources {
            enable = true
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Theme"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            // The bundled fonts (Fraunces, Plus Jakarta Sans) are Compose resources.
            implementation(libs.compose.components.resources)
        }
    }
}

compose.resources {
    // `Res` under the module's package rather than the default derived from the module name.
    packageOfResClass = "ch.stenzel.tim.polleninfo.theme.resources"
}
