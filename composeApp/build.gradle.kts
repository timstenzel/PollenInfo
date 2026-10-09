import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // The shared code as an Android library; the APK is :androidApp. The namespace differs from the
    // app's, so the library's own `R` (notification channel names, the notification icon) is
    // `ch.stenzel.tim.polleninfo.shared.R`.
    android {
        namespace = "ch.stenzel.tim.polleninfo.shared"
        compileSdk = 37
        minSdk = 26

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }

        // The notification channel names and icon in src/androidMain/res, and Compose resources.
        androidResources {
            enable = true
        }

        // Runs commonTest on the JVM.
        withHostTest { }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            // `Icons.Default.*` is NOT transitive here. It resolves for the Android target through
            // material3's own dependencies and then fails to link for iOS, so the Android build
            // alone does not catch its absence — verified against compileKotlinIosSimulatorArm64.
            implementation(libs.compose.material.icons.extended)
            // The multiplatform `NavigationBackHandler`, so the alarm editor's system back can ask
            // about unsaved changes on both platforms.
            implementation(libs.navigationevent.compose)

            implementation(libs.navigation.compose)

            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            // Multiplatform instants, time zones and formatting. `java.time` does not exist in
            // shared code. Pinned to 0.6.x: 0.7 moves `Instant` / `Clock` to `kotlin.time`, which is
            // still experimental on this Kotlin version and would put an opt-in on every use.
            implementation(libs.kotlinx.datetime)
            implementation(libs.datastore.preferences)
            implementation(project(":theme"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
            // Declared explicitly rather than leant on transitively: the permission launchers and the
            // location compat shim use them directly, so a version bump elsewhere must not be able to
            // take them away.
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            // The app language (AppCompatDelegate.setApplicationLocales), which is the system's
            // per-app language setting on API 33+ and stored by AppCompat below. :androidApp's
            // MainActivity must be an AppCompatActivity for it to apply.
            implementation(libs.androidx.appcompat)
            // Push tokens and the messaging service for pollen alarms. This is the one place the app
            // needs Google Play services; location deliberately stays on the platform provider.
            // :androidApp applies the Google Services plugin that configures it.
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.messaging)
            // The Firebase Installation ID that FCM addresses this install by.
            implementation(libs.firebase.installations)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

compose.resources {
    // `Res` under the project's package root rather than the default derived from the module name.
    packageOfResClass = "ch.stenzel.tim.polleninfo.resources"
}

val checkTranslations = tasks.register<CheckTranslationsTask>("checkTranslations") {
    group = "verification"
    description = "Fails when a translation is missing, extra, blank, has other placeholders or items, or uses ß."
    stringFiles.from(
        fileTree("src/commonMain/composeResources") { include("values*/strings.xml") },
        fileTree("src/androidMain/res") { include("values*/strings.xml") },
    )
    languages.set(listOf("de", "fr", "it"))
    marker.set(layout.buildDirectory.file("checkTranslations/ok"))
}

tasks.named("check") { dependsOn(checkTranslations) }

/**
 * Compares every `values-<lang>/strings.xml` with the English `values/strings.xml` next to it — the
 * Compose resources and the Android resources each form their own set. Fails, listing every problem,
 * when a key is missing or extra in a language, a key's placeholders differ (as a multiset), a
 * string array has another number of items, a value is blank, or a German value contains "ß" (Swiss Standard German writes "ss").
 *
 * Keys starting with `example_` (the English-only reference feature) and `translatable="false"`
 * keys are exempt from the parity check. Inputs are plain files, so the task is
 * configuration-cache compatible.
 */
abstract class CheckTranslationsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val stringFiles: ConfigurableFileCollection

    @get:Input
    abstract val languages: ListProperty<String>

    @get:OutputFile
    abstract val marker: RegularFileProperty

    private class Entry(
        val placeholders: List<String>,
        val blank: Boolean,
        val exempt: Boolean,
        val text: String,
        val items: Int?,
    )

    @TaskAction
    fun check() {
        val problems = mutableListOf<String>()
        for ((root, files) in stringFiles.files.groupBy { it.parentFile.parentFile }.toSortedMap()) {
            val byDir = files.associateBy { it.parentFile.name }
            val base = byDir["values"]
            if (base == null) {
                problems += "${root.name}: no values/strings.xml to compare against"
                continue
            }
            val baseEntries = parse(base)
            baseEntries.forEach { (key, entry) ->
                if (entry.blank) problems += "${label(base)}: '$key' is blank"
            }
            val required = baseEntries.filterValues { !it.exempt }
            val dirs = (languages.get().map { "values-$it" } + byDir.keys.filter { it != "values" }).distinct()
            for (dir in dirs) {
                val file = byDir[dir]
                if (file == null) {
                    if (required.isNotEmpty()) problems += "${root.name}/$dir/strings.xml is missing"
                    continue
                }
                val entries = parse(file)
                (required.keys - entries.keys).sorted().forEach { problems += "${label(file)}: missing '$it'" }
                entries.forEach { (key, entry) ->
                    val english = baseEntries[key]
                    when {
                        english == null -> problems += "${label(file)}: '$key' is not in values/strings.xml"
                        english.exempt -> problems += "${label(file)}: '$key' is not translated and must not appear here"
                        entry.placeholders != english.placeholders ->
                            problems += "${label(file)}: '$key' has placeholders ${entry.placeholders}, English has ${english.placeholders}"
                        entry.items != english.items ->
                            problems += "${label(file)}: '$key' has ${entry.items} items, English has ${english.items}"
                    }
                    if (entry.blank) problems += "${label(file)}: '$key' is blank"
                    if (dir.startsWith("values-de") && 'ß' in entry.text) problems += "${label(file)}: '$key' contains ß, write ss"
                }
            }
        }
        if (problems.isNotEmpty()) {
            throw GradleException("Translation check failed:\n" + problems.joinToString("\n") { "  - $it" })
        }
        marker.get().asFile.apply { parentFile.mkdirs() }.writeText("ok")
    }

    private fun label(file: java.io.File) = "${file.parentFile.parentFile.name}/${file.parentFile.name}/${file.name}"

    /**
     * `string`, `plurals` and `string-array` by name; a plural's or array's placeholders are those of
     * all its items together, and an array's item count must match English too.
     */
    private fun parse(file: java.io.File): Map<String, Entry> {
        val document = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.documentElement.childNodes
        val result = linkedMapOf<String, Entry>()
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as? org.w3c.dom.Element ?: continue
            if (element.tagName !in setOf("string", "plurals", "string-array")) continue
            val name = element.getAttribute("name")
            val texts = if (element.tagName == "string") {
                listOf(element.textContent)
            } else {
                val items = element.getElementsByTagName("item")
                (0 until items.length).map { items.item(it).textContent }
            }
            result[name] = Entry(
                placeholders = texts.flatMap { text -> PLACEHOLDER.findAll(text.replace("%%", "")).map { it.value } }.sorted(),
                blank = texts.isEmpty() || texts.any { it.isBlank() },
                exempt = name.startsWith("example_") || element.getAttribute("translatable") == "false",
                text = texts.joinToString("\n"),
                items = if (element.tagName == "string-array") texts.size else null,
            )
        }
        return result
    }

    private companion object {
        val PLACEHOLDER = Regex("""%(\d+\$)?[a-zA-Z]""")
    }
}
