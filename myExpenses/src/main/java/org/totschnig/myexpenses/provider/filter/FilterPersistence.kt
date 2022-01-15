package org.totschnig.myexpenses.provider.filter

import android.os.Bundle
import java.time.format.DateTimeParseException
import org.totschnig.myexpenses.preference.PrefHandler
import timber.log.Timber

const val KEY_FILTER = "filter"

class FilterPersistence(val prefHandler: PrefHandler, private val keyTemplate: String, savedInstanceState: Bundle?, val immediatePersist: Boolean, restoreFromPreferences: Boolean = true) {
    val whereFilter: WhereFilter
    init {
        whereFilter = savedInstanceState?.getParcelableArrayList<Criteria>(KEY_FILTER)?.let {
            WhereFilter(it)
        } ?: WhereFilter.empty().apply { if (restoreFromPreferences) restoreFromPreferences() }
    }

    private fun WhereFilter.restoreColumn(column: String, producer: (String) -> Criteria?) {
        val prefNameForCriteria = prefNameForCriteria(column)
        prefHandler.getString(prefNameForCriteria, null)?.let { prefValue ->
            producer(prefValue)?.let {
                put(it)
            } ?: kotlin.run {
                prefHandler.remove(prefNameForCriteria)
            }
        }
    }

    private fun WhereFilter.restoreFromPreferences() {
        restoreColumn(CategoryCriteria.COLUMN) {
            CategoryCriteria.fromStringExtra(it)
        }
        restoreColumn(AmountCriteria.COLUMN) {
            AmountCriteria.fromStringExtra(it)
        }
        restoreColumn(CommentCriteria.COLUMN) {
            CommentCriteria.fromStringExtra(it)
        }
        restoreColumn(CrStatusCriteria.COLUMN) {
            CrStatusCriteria.fromStringExtra(it)
        }
        restoreColumn(PayeeCriteria.COLUMN) {
            PayeeCriteria.fromStringExtra(it)
        }
        restoreColumn(MethodCriteria.COLUMN) {
            MethodCriteria.fromStringExtra(it)
        }
        restoreColumn(DateCriteria.COLUMN) {
            try {
                DateCriteria.fromStringExtra(it)
            } catch (e: DateTimeParseException) {
                Timber.e(e)
                null
            }
        }
        restoreColumn(TRANSFER_COLUMN) {
            TransferCriteria.fromStringExtra(it)
        }
        restoreColumn(TAG_COLUMN) {
            TagCriteria.fromStringExtra(it)
        }
        restoreColumn(ACCOUNT_COLUMN) {
            AccountCriteria.fromStringExtra(it)
        }
    }

    fun addCriteria(criteria: Criteria) {
        whereFilter.put(criteria)
        if (immediatePersist) {
            persist(criteria)
        }
    }

    fun removeFilter(id: Int) : Boolean = whereFilter.get(id)?.let {
        whereFilter.remove(id)
        if (immediatePersist) {
            prefHandler.remove(prefNameForCriteria(it.column))
        }
        true
    } ?: false

    fun persistAll() {
        arrayOf(CategoryCriteria.COLUMN, AmountCriteria.COLUMN, CommentCriteria.COLUMN,
                CrStatusCriteria.COLUMN, PayeeCriteria.COLUMN, MethodCriteria.COLUMN,
                DateCriteria.COLUMN, TRANSFER_COLUMN, TAG_COLUMN, ACCOUNT_COLUMN).forEach { column ->
            whereFilter.get(column)?.let {
                persist(it)
            } ?: kotlin.run { prefHandler.remove(prefNameForCriteria(column)) }
        }
    }

    private fun persist(criteria: Criteria) {
        prefHandler.putString(prefNameForCriteria(criteria.column), criteria.toStringExtra())
    }

    private fun prefNameForCriteria(columnName: String) = keyTemplate.format(columnName)

    fun clearFilter() {
        if (immediatePersist) {
            whereFilter.criteria.forEach { criteria -> prefHandler.remove(prefNameForCriteria(criteria.column)) }
        }
        whereFilter.clear()
    }

    fun reloadFromPreferences() {
        with(whereFilter) {
            clear()
            restoreFromPreferences()
        }
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(KEY_FILTER, whereFilter.criteria)
    }
}