package org.totschnig.myexpenses.provider.filter

import android.os.Bundle
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import timber.log.Timber
import java.time.format.DateTimeParseException

const val KEY_FILTER = "filter"

class FilterPersistence(val prefHandler: PrefHandler, private val keyTemplate: String, savedInstanceState: Bundle?, val immediatePersist: Boolean, restoreFromPreferences: Boolean = true) {
    val whereFilter: WhereFilter
    init {
        whereFilter = savedInstanceState?.getParcelableArrayList<Criteria<*>>(KEY_FILTER)?.let {
            WhereFilter(it)
        } ?: WhereFilter.empty().apply { if (restoreFromPreferences) restoreFromPreferences() }
    }

    private fun WhereFilter.restoreColumn(column: String, producer: (String) -> Criteria<*>?) {
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
        restoreColumn(KEY_CATID) {
            CategoryCriteria.fromStringExtra(it)
        }
        restoreColumn(KEY_AMOUNT) {
            AmountCriteria.fromStringExtra(it)
        }
        restoreColumn(KEY_COMMENT) {
            CommentCriteria.fromStringExtra(it)
        }
        restoreColumn(KEY_CR_STATUS) {
            CrStatusCriteria.fromStringExtra(it)
        }
        restoreColumn(KEY_PAYEEID) {
            PayeeCriteria.fromStringExtra(it)
        }
        restoreColumn(KEY_METHODID) {
            MethodCriteria.fromStringExtra(it)
        }
        restoreColumn(KEY_DATE) {
            try {
                DateCriteria.fromStringExtra(it)
            } catch (e: DateTimeParseException) {
                Timber.e(e)
                null
            }
        }
        restoreColumn(KEY_TRANSFER_ACCOUNT) {
            TransferCriteria.fromStringExtra(it)
        }
        restoreColumn(KEY_TAGID) {
            TagCriteria.fromStringExtra(it)
        }
        restoreColumn(ACCOUNT_COLUMN) {
            AccountCriteria.fromStringExtra(it)
        }
    }

    fun addCriteria(criteria: Criteria<*>) {
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
        arrayOf(
            KEY_CATID, KEY_AMOUNT, KEY_COMMENT, KEY_CR_STATUS, KEY_PAYEEID, KEY_METHODID, KEY_DATE,
            KEY_TRANSFER_ACCOUNT, KEY_TAGID, ACCOUNT_COLUMN
        ).forEach { column ->
            whereFilter.get(column)?.let {
                persist(it)
            } ?: kotlin.run { prefHandler.remove(prefNameForCriteria(column)) }
        }
    }

    private fun persist(criteria: Criteria<*>) {
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