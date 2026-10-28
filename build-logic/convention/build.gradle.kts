plugins {
    `kotlin-dsl`
}

// Only compileOnly: the root build.gradle puts the plugins on the classpath (apply false),
// so that all modules use the same versions.
dependencies {
    compileOnly("com.android.tools.build:gradle:${libs.versions.androidGradlePlugin.get()}")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "next.android.library"
            implementationClass = "org.totschnig.myexpenses.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "next.android.feature"
            implementationClass = "org.totschnig.myexpenses.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("androidCompose") {
            id = "next.android.compose"
            implementationClass = "org.totschnig.myexpenses.buildlogic.AndroidComposeConventionPlugin"
        }
        register("feature") {
            id = "next.feature"
            implementationClass = "org.totschnig.myexpenses.buildlogic.FeatureConventionPlugin"
        }
        register("data") {
            id = "next.data"
            implementationClass = "org.totschnig.myexpenses.buildlogic.DataConventionPlugin"
        }
        register("jvm") {
            id = "next.jvm"
            implementationClass = "org.totschnig.myexpenses.buildlogic.JvmConventionPlugin"
        }
        register("moduleGraph") {
            id = "next.module-graph"
            implementationClass = "org.totschnig.myexpenses.buildlogic.ModuleGraphPlugin"
        }
    }
}
