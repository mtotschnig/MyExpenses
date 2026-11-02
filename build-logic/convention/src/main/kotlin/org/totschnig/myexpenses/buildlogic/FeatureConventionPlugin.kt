package org.totschnig.myexpenses.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * A feature of the new UI, e.g. `:next:feature:contracts`: the screens and view models of an area,
 * an Android library with Compose on the core and data modules, never on another feature or on
 * `:myExpenses` (rule `mod-feature-isolated`). What it needs of the app it gets through
 * interfaces, which the shell `:ui_next` implements; screens of other features the shell hands in
 * as composable parameters.
 */
class FeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("next.android.library")
        pluginManager.apply("next.android.compose")
        dependencies {
            add("implementation", project(":next:core:ui"))
            listOf(
                "androidx-activity-compose",
                "androidx-lifecycle-runtime-compose",
                "androidx-lifecycle-viewmodel-ktx",
                "androidx-material-icons-extended",
                "androidx-datastore-preferences",
                // Pages of a feature are entries of a back stack (rule `screen-nav3-stack`)
                "androidx-navigation3-runtime",
                "androidx-navigation3-ui",
            ).forEach { add("implementation", libs.findLibrary(it).get()) }

            add("testImplementation", libs.findLibrary("junit").get())
            // The real implementation, android.jar only has stubs for org.json
            add("testImplementation", libs.findLibrary("org-json").get())
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
            add("testImplementation", testFixtures(project(":next:core:model")))
            add("testImplementation", testFixtures(project(":next:core:common")))
        }
        forbidProjectDependencies("mod-feature-isolated: a feature depends on no other feature, the shell or the app") {
            it.startsWith(":next:feature:") || it == ":ui_next" || it == ":myExpenses"
        }
    }
}
