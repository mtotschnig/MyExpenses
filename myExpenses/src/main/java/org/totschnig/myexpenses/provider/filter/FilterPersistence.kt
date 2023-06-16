package org.totschnig.myexpenses.provider.filter

import android.os.Bundle
import androidx.annotation.CheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import timber.log.Timber
import java.time.format.DateTimeParseException

const val KEY_FILTER = "filter"

class FilterPersistence(
    val prefHandler: PrefHandler,
    private val keyTemplate: String,
    savedInstanceState: Bundle?,
    val immediatePersist: Boolean,
    restoreFromPreferences: Boolean = true
) {
    val whereFilterAsFlow: StateFlow<WhereFilter>
        get() = _whereFilter
    private val _whereFilter: MutableStateFlow<WhereFilter>
    val whereFilter: WhereFilter
        get() = _whereFilter.value

    init {
        _whereFilter = MutableStateFlow(
            savedInstanceState?.getParcelableArrayList<Criterion<*>>(KEY_FILTER)?.let {
                WhereFilter(it)
            } ?: if (restoreFromPreferences) restoreFromPreferences() else WhereFilter.empty()
        )
    }

    @CheckResult
    private fun WhereFilter.restoreColumn(
        column: String,
        producer: (String) -> Criterion<*>?
    ): WhereFilter {
        val prefNameForCriteria = prefNameForCriteria(column)
        return prefHandler.getString(prefNameForCriteria, null)?.let { prefValue ->
            producer(prefValue)?.let {
                put(it)
            } ?: kotlin.run {
                prefHandler.remove(prefNameForCriteria)
                this
            }
        } ?: this
    }

    @CheckResult
    private fun restoreFromPreferences() = WhereFilter.empty()
        .restoreColumn(KEY_CATID) {
            CategoryCriterion.fromStringExtra(it)
        }
        .restoreColumn(KEY_AMOUNT) {
            AmountCriterion.fromStringExtra(it)
        }
        .restoreColumn(KEY_COMMENT) {
            CommentCriterion.fromStringExtra(it)
        }
        .restoreColumn(KEY_CR_STATUS) {
            CrStatusCriterion.fromStringExtra(it)
        }
        .restoreColumn(KEY_PAYEEID) {
            PayeeCriterion.fromStringExtra(it)
        }
        .restoreColumn(KEY_METHODID) {
            MethodCriterion.fromStringExtra(it)
        }
        .restoreColumn(KEY_DATE) {
            try {
                DateCriterion.fromStringExtra(it)
            } catch (e: DateTimeParseException) {
                Timber.e(e)
                null
            }
        }
        .restoreColumn(KEY_TRANSFER_ACCOUNT) {
            TransferCriterion.fromStringExtra(it)
        }
        .restoreColumn(KEY_TAGID) {
            TagCriterion.fromStringExtra(it)
        }
        .restoreColumn(ACCOUNT_COLUMN) {
            AccountCriterion.fromStringExtra(it)
        }

    fun addCriteria(criterion: Criterion<*>) {
        _whereFilter.value = whereFilter.put(criterion)
        if (immediatePersist) {
            persist(criterion)
        }
    }

    fun removeFilter(id: Int): Boolean = whereFilter[id]?.let {
        _whereFilter.value = whereFilter.remove(id)
        if (immediatePersist) {
            prefHandler.remove(prefNameForCriteria(it))
        }
        true
    } ?: false

    fun persistAll() {
        arrayOf(
            KEY_CATID, KEY_AMOUNT, KEY_COMMENT, KEY_CR_STATUS, KEY_PAYEEID, KEY_METHODID, KEY_DATE,
            KEY_TRANSFER_ACCOUNT, KEY_TAGID, ACCOUNT_COLUMN
        ).forEach { column ->
            whereFilter[column]?.let {
                persist(it)
            } ?: kotlin.run { prefHandler.remove(prefNameForCriteria(column)) }
        }
    }

    private fun persist(criterion: Criterion<*>) {
        prefHandler.putString(prefNameForCriteria(criterion), criterion.toString())
    }

    private fun prefNameForCriteria(criterion: Criterion<*>) = prefNameForCriteria(criterion.key)

    private fun prefNameForCriteria(columnName: String) = keyTemplate.format(columnName)

    fun clear() {
        if (immediatePersist) {
            whereFilter.criteria.forEach { criteria ->
                prefHandler.remove(prefNameForCriteria(criteria))
            }
        }
        _whereFilter.value = WhereFilter.empty()
    }

    fun reloadFromPreferences() {
        _whereFilter.value = restoreFromPreferences()
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(KEY_FILTER, ArrayList(whereFilter.criteria))
    }
}