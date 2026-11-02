package org.totschnig.myexpenses.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/** Rules mod-ds-pure, mod-own-packages and mod-shell-boundary of ui_next/ARCHITECTURE.md. */
class ModuleRulesTest {

    @Test
    fun `mod-ds-pure designsystem knows no type of the app`() {
        Konsist.scopeFromProduction(moduleName = "designsystem")
            .imports
            .assertFalse(additionalMessage = "mod-ds-pure: :designsystem knows only strings, numbers, icons and lambdas") {
                it.name.startsWith(APP) &&
                    !it.name.startsWith("$APP.myexpenses.designsystem.")
            }
    }

    @Test
    fun `mod-own-packages each module below next has packages of its own`() {
        nextScope.files
            .filter { it.projectPath.startsWith("/next/") }
            .assertTrue(additionalMessage = "mod-own-packages: the package of a module starts with its namespace") { file ->
                // /next/<kind>/<name>/src/… -> org.totschnig.myexpenses.next.<kind>.<name>
                val (kind, name) = file.projectPath.split('/').drop(2).take(2)
                file.packagee?.name?.startsWith("$APP.myexpenses.next.$kind.${name.replace('-', '_')}") == true
            }
    }

    @Test
    fun `mod-shell-boundary the shell knows an area only in its package`() {
        // The areas are the modules below next/data and next/feature, e.g. contracts
        val areas = nextScope.files.map { it.projectPath.split('/') }
            .filter { it.getOrNull(1) == "next" && it.getOrNull(2) in setOf("data", "feature") }
            .mapNotNull { it.getOrNull(3) }
            .toSet()
        nextScope.files
            .filter { it.projectPath.startsWith("/ui_next/src/main/") && it.name !in SHELL_WIRING }
            .filterNot { file -> file.packagee?.name?.removePrefix("$APP.myexpenses.next.")?.substringBefore('.') in areas }
            .flatMap { it.imports }
            .assertFalse(additionalMessage = "mod-shell-boundary: only the package of an area, NextFeatures and NextData import its modules") {
                it.name.startsWith("$APP.myexpenses.next.feature.") || it.name.startsWith("$APP.myexpenses.next.data.")
            }
    }

    private companion object {
        const val APP = "org.totschnig"

        /** The files of the shell that list the areas */
        val SHELL_WIRING = setOf("NextFeatures", "NextData")
    }
}
