package org.totschnig.myexpenses.architecture

import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/** Rules screen-no-alert-dialog, vm-constructor, vm-entry-points and vm-io-param of ui_next/ARCHITECTURE.md. */
class UiNextRulesTest {

    private val scope = nextScope

    @Test
    fun `screen-no-alert-dialog no AlertDialog in ui_next`() {
        scope.imports
            .assertFalse(additionalMessage = "screen-no-alert-dialog: dialogs come from the dialogs of the design system") {
                it.name.endsWith(".AlertDialog")
            }
    }

    @Test
    fun `vm-constructor view models get their dependencies through the constructor`() {
        scope.classes()
            .filter { it.isViewModel() }
            .assertTrue(additionalMessage = "vm-constructor: no field injection, no by lazy, no start()") { viewModel ->
                viewModel.properties().none { it.hasAnnotationWithName("Inject") || it.hasDelegate("lazy") } &&
                    viewModel.functions().none { it.name == "start" }
            }
    }

    @Test
    fun `vm-entry-points view models are imported only by entry points`() {
        scope.files
            .filterNot { it.isEntryPoint() }
            .flatMap { it.imports }
            .assertFalse(additionalMessage = "vm-entry-points: only MyExpensesNext, NextSettings, the …Feature entries of the shell and the …Flow of a feature import view models") {
                VIEW_MODEL_OF_NEXT.matches(it.name)
            }
    }

    @Test
    fun `vm-io-param view models take their IO dispatcher as a parameter`() {
        scope.classes()
            .filter { it.isViewModel() }
            .assertFalse(additionalMessage = "vm-io-param: take `io: CoroutineDispatcher = Dispatchers.IO` as a parameter, so that tests can run the view model") { viewModel ->
                val constructor = viewModel.primaryConstructor?.text.orEmpty()
                viewModel.text.replace(constructor, "").contains("Dispatchers.IO")
            }
    }

    private fun KoClassDeclaration.isViewModel() =
        parents().any { it.name == "ViewModel" || it.name == "AndroidViewModel" }

    private fun KoFileDeclaration.isEntryPoint() =
        name in ENTRY_POINTS || name.endsWith("Flow") || name.endsWith("Feature")

    private companion object {
        val ENTRY_POINTS = setOf("MyExpensesNext", "NextSettings")
        val VIEW_MODEL_OF_NEXT = Regex("""org\.totschnig\.myexpenses\.next(\.\w+)+\.\w+ViewModel""")
    }
}
