package org.totschnig.myexpenses.provider.filter

import android.os.Bundle
import org.threeten.bp.format.DateTimeParseException
import org.totschnig.myexpenses.preference.PrefHandler
import timber.log.Timber

const val KEY_FILTER = "filter"

class FilterPersistence(val prefHandler: PrefHandler, private val keyTemplate: String, savedInstanceState: Bundle?, val immediatePersist: Boolean, restoreFromPreferences: Boolean = true) {
    val whereFilter: WhereFilter
    init {
        whereFilter = savedInstanceState?.getParcelableArrayList<Criteria>(KEY_FILTER)?.let {
            WhereFilter(it)
        } ?: WhereFilter.empty().apply { if (restoreFromPreferences) restoreFromPreferences(this) }
    }

    private fun restoreFromPreferences(whereFilter: WhereFilter) {
        prefHandler.getString(prefNameForCriteria(CategoryCriteria.COLUMN), null)?.let {
            whereFilter.put(CategoryCriteria.fromStringExtra(it))
        }
        prefHandler.getString(prefNameForCriteria(AmountCriteria.COLUMN), null)?.let {
            whereFilter.put(AmountCriteria.fromStringExtra(it))
        }
        prefHandler.getString(prefNameForCriteria(CommentCriteria.COLUMN), null)?.let {
            whereFilter.put(CommentCriteria.fromStringExtra(it))
        }
        prefHandler.getString(prefNameForCriteria(CrStatusCriteria.COLUMN), null)?.let {
            whereFilter.put(CrStatusCriteria.fromStringExtra(it))
        }
        prefHandler.getString(prefNameForCriteria(PayeeCriteria.COLUMN), null)?.let {
            whereFilter.put(PayeeCriteria.fromStringExtra(it))
        }
        prefHandler.getString(prefNameForCriteria(MethodCriteria.COLUMN), null)?.let {
            whereFilter.put(MethodCriteria.fromStringExtra(it))
        }
        prefHandler.getString(prefNameForCriteria(DateCriteria.COLUMN), null)?.let {
            try {
                whereFilter.put(DateCriteria.fromStringExtra(it))
            } catch (e: DateTimeParseException) {
                Timber.e(e)
            }
        }
        prefHandler.getString(prefNameForCriteria(TRANSFER_COLUMN), null)?.let {
            whereFilter.put(TransferCriteria.fromStringExtra(it))
        }
        prefHandler.getString(prefNameForCriteria(TAG_COLUMN), null)?.let {
            whereFilter.put(TagCriteria.fromStringExtra(it))
        }
        prefHandler.getString(prefNameForCriteria(ACCOUNT_COLUMN), null)?.let {
            whereFilter.put(AccountCriteria.fromStringExtra(it))
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
        whereFilter.let {
            it.clear()
            restoreFromPreferences(it)
        }
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(KEY_FILTER, whereFilter.criteria)
    }
}