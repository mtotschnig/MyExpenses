package org.totschnig.myexpenses.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

/**
 * Fails the configuration when a configuration of this module declares a dependency on a module
 * whose path matches [forbidden], naming the [rule]. Test configurations are not checked.
 */
internal fun Project.forbidProjectDependencies(rule: String, forbidden: (path: String) -> Boolean) {
    configurations.configureEach {
        if (name.contains("test", ignoreCase = true)) return@configureEach
        val configuration = name
        dependencies.withType(ProjectDependency::class.java).configureEach {
            if (forbidden(path)) {
                throw GradleException("$rule, but ${this@forbidProjectDependencies.path} declares $configuration $path")
            }
        }
    }
}
