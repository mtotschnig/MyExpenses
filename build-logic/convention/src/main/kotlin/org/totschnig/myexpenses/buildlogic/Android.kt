package org.totschnig.myexpenses.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val javaVersion = JavaVersion.VERSION_17

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** SDK levels come from the ext block of the root build.gradle, which upstream maintains. */
internal fun Project.configureAndroid(android: CommonExtension) {
    android.apply {
        compileSdk = rootProject.extensions.extraProperties["compileSdkVersion"] as Int
        defaultConfig.minSdk = rootProject.extensions.extraProperties["minSdkVersion"] as Int
        compileOptions.sourceCompatibility = javaVersion
        compileOptions.targetCompatibility = javaVersion
    }
}
