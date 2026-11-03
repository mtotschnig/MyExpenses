package org.totschnig.myexpenses.next.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/**
 * What the details of a transaction show for changing it
 *
 * @param transaction the opened transaction, null while it loads or if the details cannot change
 * it (see [TransactionStore.load])
 * @param methods payment methods to choose from, for the sign of the transaction or of the draft
 * @param draft what is typed in the edit mode, null outside of it
 * @param isAmountInvalid the amount of the draft cannot be read, it is not saved then
 * @param undo the last quick change, offered to undo until it was shown
 */
data class TransactionEditState(
    val transaction: EditableTransaction? = null,
    val methods: List<MethodOption> = emptyList(),
    val draft: EditDraft? = null,
    val isAmountInvalid: Boolean = false,
    val undo: Undo? = null,
)

/** What the user can do in the details of a transaction to change it */
interface TransactionEditActions {
    /** Loads [transactionId] when its details open */
    fun open(transactionId: Long)

    fun close()

    /** Quick changes, saved at once and offered to undo */
    fun changeComment(comment: String)
    fun changeDay(day: LocalDate)
    fun changeMethod(methodId: Long?)
    fun undo()
    fun undoShown()

    /** The edit mode with all fields at once, saved only on [saveDraft] */
    fun startEditing()
    fun changeDraft(draft: EditDraft)
    fun saveDraft()
    fun cancelEditing()
}

/**
 * Changes a transaction from its details, see [TransactionEditActions]. The draft of the edit mode
 * is kept in [savedState], so that it survives when Android ends the process.
 */
class TransactionEditViewModel(
    private val store: TransactionStore,
    private val savedState: SavedStateHandle,
    private val decimalSeparator: Char,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel(), TransactionEditActions {

    private val _state = MutableStateFlow(TransactionEditState(draft = savedState[KEY_DRAFT]))
    val state: StateFlow<TransactionEditState> = _state.asStateFlow()

    private var openedId: Long? = null
    private var loadJob: Job? = null

    override fun open(transactionId: Long) {
        if (openedId == transactionId) return
        openedId = transactionId
        // A draft only belongs to the transaction it was started for
        if (_state.value.draft?.id != transactionId) setDraft(null)
        _state.update { it.copy(transaction = null, methods = emptyList(), undo = null) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch { reload(transactionId) }
    }

    override fun close() {
        openedId = null
        loadJob?.cancel()
        setDraft(null)
        _state.value = TransactionEditState()
    }

    override fun changeComment(comment: String) =
        quickChange(ChangedField.COMMENT) { it.copy(comment = comment.trim().ifEmpty { null }) }

    override fun changeDay(day: LocalDate) = quickChange(ChangedField.DATE) { it.withDay(day, zone()) }

    override fun changeMethod(methodId: Long?) = quickChange(ChangedField.METHOD) { it.copy(methodId = methodId) }

    override fun undo() {
        val undo = _state.value.undo ?: return
        _state.update { it.copy(undo = null) }
        viewModelScope.launch {
            withContext(io) { store.save(undo.id, undo.previous) }
            if (openedId == undo.id) reload(undo.id)
        }
    }

    override fun undoShown() = _state.update { it.copy(undo = null) }

    override fun startEditing() {
        val transaction = _state.value.transaction ?: return
        setDraft(transaction.toDraft(decimalSeparator, zone()))
    }

    override fun changeDraft(draft: EditDraft) {
        val signChanged = draft.isIncome != _state.value.draft?.isIncome
        setDraft(draft)
        _state.update { it.copy(isAmountInvalid = false) }
        if (signChanged) _state.value.transaction?.let { loadMethods(it, draft.isIncome) }
    }

    override fun saveDraft() {
        val transaction = _state.value.transaction ?: return
        val draft = _state.value.draft ?: return
        val values = transaction.apply(draft, zone())
        if (values == null) {
            _state.update { it.copy(isAmountInvalid = true) }
            return
        }
        setDraft(null)
        save(transaction, values, ChangedField.ALL)
    }

    override fun cancelEditing() {
        setDraft(null)
        _state.value.transaction?.let { loadMethods(it, it.values.amount > 0) }
    }

    private fun quickChange(field: ChangedField, change: (EditValues) -> EditValues) {
        val transaction = _state.value.transaction ?: return
        val values = change(transaction.values)
        if (values != transaction.values) save(transaction, values, field)
    }

    private fun save(transaction: EditableTransaction, values: EditValues, field: ChangedField) {
        // Shown at once, the list catches up when the database tells it
        _state.update {
            it.copy(
                transaction = transaction.copy(values = values),
                undo = Undo(transaction.id, transaction.values, field)
            )
        }
        viewModelScope.launch {
            val saved = withContext(io) { runCatching { store.save(transaction.id, values) } }
            if (saved.isFailure) {
                // Show what the database has, nothing to undo
                _state.update { it.copy(undo = null) }
                if (openedId == transaction.id) reload(transaction.id)
            } else if (transaction.values.amount > 0 != values.amount > 0) {
                loadMethods(transaction, values.amount > 0)
            }
        }
    }

    private suspend fun reload(id: Long) {
        val transaction = withContext(io) { store.load(id) }
        if (openedId != id) return
        _state.update { it.copy(transaction = transaction) }
        if (transaction == null) setDraft(null)
        else loadMethods(transaction, _state.value.draft?.isIncome ?: (transaction.values.amount > 0))
    }

    private fun loadMethods(transaction: EditableTransaction, isIncome: Boolean) {
        val accountType = transaction.accountType ?: return
        viewModelScope.launch {
            val methods = withContext(io) { store.methods(accountType, isIncome) }
            if (openedId == transaction.id) _state.update { it.copy(methods = methods) }
        }
    }

    private fun setDraft(draft: EditDraft?) {
        savedState[KEY_DRAFT] = draft
        _state.update { it.copy(draft = draft, isAmountInvalid = it.isAmountInvalid && draft != null) }
    }

    private companion object {
        const val KEY_DRAFT = "transactionEditDraft"
    }
}
