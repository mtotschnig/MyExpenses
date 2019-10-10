package org.totschnig.myexpenses.provider.filter

import android.os.Bundle
import org.totschnig.myexpenses.preference.PrefHandler

const val KEY_FILTER = "filter"

class FilterPersistence(val prefHandler: PrefHandler, val keyTemplate: String, savedInstanceState: Bundle?) {
    val whereFilter: WhereFilter
    init {
        if (savedInstanceState == null) {
            whereFilter = WhereFilter.empty()
            restoreFromPreferences()
        } else {
            whereFilter = WhereFilter(savedInstanceState.getParcelableArrayList(KEY_FILTER))
        }
    }

    private fun restoreFromPreferences() {
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
            whereFilter.put(DateCriteria.fromStringExtra(it))
        }
        prefHandler.getString(prefNameForCriteria(TransferCriteria.COLUMN), null)?.let {
            whereFilter.put(TransferCriteria.fromStringExtra(it))
        }
    }

    fun addCriteria(criteria: Criteria, persist: Boolean) {
        whereFilter.put(criteria)
        if (persist) {
            persist(criteria)
        }
    }

    fun persistAll() {
        arrayOf(CategoryCriteria.COLUMN, AmountCriteria.COLUMN, CommentCriteria.COLUMN,
                CrStatusCriteria.COLUMN, PayeeCriteria.COLUMN, MethodCriteria.COLUMN,
                DateCriteria.COLUMN, TransferCriteria.COLUMN).forEach {
            whereFilter.get(it)?.let {
                persist(it)
            } ?: kotlin.run { prefHandler.remove(prefNameForCriteria(it)) }
        }
    }

    private fun persist(criteria: Criteria) {
        prefHandler.putString(prefNameForCriteria(criteria.column), criteria.toStringExtra())
    }

    private fun prefNameForCriteria(columnName: String) = keyTemplate.format(columnName)

    fun removeFilter(id: Int) : Boolean = whereFilter.get(id)?.let {
        prefHandler.remove(prefNameForCriteria(it.column))
        whereFilter.remove(id)
        true
    } ?: false

    fun clearFilter() {
        whereFilter.criteria.forEach { criteria -> prefHandler.remove(prefNameForCriteria(criteria.column)) }
        whereFilter.clear()
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(KEY_FILTER, whereFilter.getCriteria())
    }
}