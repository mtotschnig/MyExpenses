package org.totschnig.myexpenses.buildlogic

import com.android.build.api.dsl.DynamicFeatureExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * A dynamic feature of the new UI on top of `:myExpenses`, e.g. `:ui_next`. Like the main module it
 * uses java.time through desugaring.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.dynamic-feature")
        extensions.configure<DynamicFeatureExtension> {
            configureAndroid(this)
            defaultConfig.missingDimensionStrategy("textRecognition", "extern")
            compileOptions.isCoreLibraryDesugaringEnabled = true
        }
        dependencies {
            add("implementation", project(":myExpenses"))
            add("coreLibraryDesugaring", libs.findLibrary("android-desugar").get())
        }
    }
}
