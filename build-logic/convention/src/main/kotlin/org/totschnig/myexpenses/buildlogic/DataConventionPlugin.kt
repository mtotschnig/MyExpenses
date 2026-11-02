package org.totschnig.myexpenses.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * The data of an area of the new UI, e.g. `:next:data:contracts`: its facts, analyses, stores and
 * what it needs of the app as interfaces. An Android library without Compose on the core modules
 * and on data modules of other areas, never on a feature, on `:next:core:ui` or on the app, so
 * that widgets and notifications can use it without a screen (rule `mod-data-headless`).
 */
class DataConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("next.android.library")
        dependencies {
            add("api", project(":next:core:common"))
            add("testImplementation", libs.findLibrary("junit").get())
            // The real implementation, android.jar only has stubs for org.json
            add("testImplementation", libs.findLibrary("org-json").get())
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
            add("testImplementation", testFixtures(project(":next:core:model")))
            add("testImplementation", testFixtures(project(":next:core:common")))
        }
        forbidProjectDependencies("mod-data-headless: a data module depends only on core and data modules and on modules without app types") {
            it.startsWith(":next:feature:") || it == ":next:core:ui" || it == ":ui_next" || it == ":myExpenses" ||
                it == ":designsystem" || it == ":charts"
        }
    }
}
