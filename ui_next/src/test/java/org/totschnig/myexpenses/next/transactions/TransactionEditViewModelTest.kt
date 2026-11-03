package org.totschnig.myexpenses.next.transactions

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionEditViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private class FakeStore(var stored: EditableTransaction?) : TransactionStore {
        val saved = mutableListOf<EditValues>()
        val methodsAsked = mutableListOf<Boolean>()

        override suspend fun load(id: Long) = stored?.takeIf { it.id == id }

        override suspend fun methods(accountType: Long, isIncome: Boolean): List<MethodOption> {
            methodsAsked += isIncome
            return listOf(MethodOption(if (isIncome) 2 else 1, if (isIncome) "Gutschrift" else "Lastschrift"))
        }

        override suspend fun save(id: Long, values: EditValues) {
            saved += values
            stored = stored?.copy(values = values)
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(store: TransactionStore, savedState: SavedStateHandle = SavedStateHandle()) =
        TransactionEditViewModel(store, savedState, ',', { ZoneOffset.UTC }, dispatcher)

    @Test
    fun loadsTransactionAndMethodsForItsSign() {
        val store = FakeStore(transaction())
        val viewModel = viewModel(store)
        viewModel.open(1)
        assertEquals(transaction(), viewModel.state.value.transaction)
        assertEquals(listOf(false), store.methodsAsked)
        assertEquals("Lastschrift", viewModel.state.value.methods.single().label)
    }

    @Test
    fun nothingToEditForWhatTheStoreLeavesToTheClassicForm() {
        val viewModel = viewModel(FakeStore(null))
        viewModel.open(1)
        assertNull(viewModel.state.value.transaction)
        viewModel.startEditing()
        assertNull(viewModel.state.value.draft)
    }

    @Test
    fun quickChangeSavesAtOnceAndCanBeUndone() {
        val store = FakeStore(transaction())
        val viewModel = viewModel(store)
        viewModel.open(1)
        viewModel.changeComment(" Wocheneinkauf ")
        assertEquals("Wocheneinkauf", store.saved.last().comment)
        assertEquals("Wocheneinkauf", viewModel.state.value.transaction!!.values.comment)
        assertEquals(ChangedField.COMMENT, viewModel.state.value.undo!!.field)

        viewModel.undo()
        assertNull(store.saved.last().comment)
        assertNull(viewModel.state.value.transaction!!.values.comment)
        assertNull(viewModel.state.value.undo)
    }

    @Test
    fun quickChangeWithoutDifferenceSavesNothing() {
        val store = FakeStore(transaction())
        val viewModel = viewModel(store)
        viewModel.open(1)
        viewModel.changeDay(LocalDate.of(2026, 3, 1))
        assertTrue(store.saved.isEmpty())
        assertNull(viewModel.state.value.undo)
    }

    @Test
    fun editModeSavesAllFieldsOnlyOnSave() {
        val store = FakeStore(transaction())
        val viewModel = viewModel(store)
        viewModel.open(1)
        viewModel.startEditing()
        val draft = viewModel.state.value.draft!!
        assertEquals("42,90", draft.amountText)

        viewModel.changeDraft(draft.copy(amountText = "50", payee = "Lidl", methodId = 1))
        assertTrue(store.saved.isEmpty())

        viewModel.saveDraft()
        assertEquals(EditValues(-5000, transaction().values.date, transaction().values.valueDate, "Lidl", 1, null), store.saved.single())
        assertNull(viewModel.state.value.draft)
        assertEquals(ChangedField.ALL, viewModel.state.value.undo!!.field)
    }

    @Test
    fun invalidAmountIsNotSaved() {
        val store = FakeStore(transaction())
        val viewModel = viewModel(store)
        viewModel.open(1)
        viewModel.startEditing()
        viewModel.changeDraft(viewModel.state.value.draft!!.copy(amountText = "4x"))
        viewModel.saveDraft()
        assertTrue(store.saved.isEmpty())
        assertTrue(viewModel.state.value.isAmountInvalid)
        assertNotNull(viewModel.state.value.draft)
    }

    @Test
    fun changingTheSignLoadsTheMethodsForIt() {
        val store = FakeStore(transaction())
        val viewModel = viewModel(store)
        viewModel.open(1)
        viewModel.startEditing()
        viewModel.changeDraft(viewModel.state.value.draft!!.copy(isIncome = true))
        assertEquals(listOf(false, true), store.methodsAsked)
        assertEquals("Gutschrift", viewModel.state.value.methods.single().label)
    }

    @Test
    fun draftSurvivesInSavedStateForItsTransactionOnly() {
        val savedState = SavedStateHandle()
        val store = FakeStore(transaction())
        viewModel(store, savedState).apply {
            open(1)
            startEditing()
            changeDraft(state.value.draft!!.copy(comment = "Notiz"))
        }
        val restored = viewModel(store, savedState)
        restored.open(1)
        assertEquals("Notiz", restored.state.value.draft!!.comment)

        store.stored = transaction(id = 2)
        restored.open(2)
        assertNull(restored.state.value.draft)
    }
}
