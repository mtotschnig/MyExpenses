package org.totschnig.myexpenses.model

import androidx.annotation.VisibleForTesting
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants

enum class Sort(val commandId: Int, private val isDescending: Boolean = true) {
    USAGES(R.id.SORT_USAGES_COMMAND), LAST_USED(R.id.SORT_LAST_USED_COMMAND), AMOUNT(R.id.SORT_AMOUNT_COMMAND), TITLE(R.id.SORT_TITLE_COMMAND, false), LABEL(R.id.SORT_LABEL_COMMAND, false), CUSTOM(R.id.SORT_CUSTOM_COMMAND, false), NEXT_INSTANCE(R.id.SORT_NEXT_INSTANCE_COMMAND), ALLOCATED(R.id.SORT_ALLOCATED_COMMAND), SPENT(R.id.SORT_SPENT_COMMAND);

    @VisibleForTesting
    fun toDatabaseColumn() = when (this) {
        USAGES -> DatabaseConstants.KEY_USAGES
        LAST_USED -> DatabaseConstants.KEY_LAST_USED
        AMOUNT -> "abs(" + DatabaseConstants.KEY_AMOUNT + ")"
        TITLE -> DatabaseConstants.KEY_TITLE + " COLLATE LOCALIZED"
        LABEL -> DatabaseConstants.KEY_LABEL + " COLLATE LOCALIZED"
        CUSTOM -> DatabaseConstants.KEY_SORT_KEY
        ALLOCATED -> DatabaseConstants.KEY_BUDGET
        SPENT -> DatabaseConstants.KEY_SUM
        else -> null
    }

    fun toOrderBy() = toDatabaseColumn()?.let {
        if (isDescending) it + " DESC" else it
    }

    companion object {
        private val categorySort = arrayOf(LABEL, USAGES, LAST_USED)
        private val templateSort = arrayOf(TITLE, USAGES, LAST_USED, AMOUNT, NEXT_INSTANCE)
        private val budgetSort = arrayOf(LABEL, ALLOCATED, SPENT)
        private val accountSort = arrayOf(LABEL, USAGES, LAST_USED, CUSTOM)

        @JvmStatic
        fun fromCommandId(id: Int): Sort? {
            for (sort in values()) {
                if (sort.commandId == id) return sort
            }
            return null
        }

        @JvmStatic
        fun preferredOrderByForBudgets(prefKey: PrefKey?, prefHandler: PrefHandler, defaultOrderBy: Sort) =
                preferredOrderByRestricted(prefKey, prefHandler, defaultOrderBy, budgetSort)

        @JvmStatic
        fun preferredOrderByForCategories(prefKey: PrefKey?, prefHandler: PrefHandler, defaultOrderBy: Sort) =
                preferredOrderByRestricted(prefKey, prefHandler, defaultOrderBy, categorySort)

        @JvmStatic
        fun preferredOrderByForTemplates(prefKey: PrefKey?, prefHandler: PrefHandler, defaultOrderBy: Sort) =
                preferredOrderByRestricted(prefKey, prefHandler, defaultOrderBy, templateSort)

        @JvmStatic
        fun preferredOrderByForAccounts(prefKey: PrefKey?, prefHandler: PrefHandler, defaultOrderBy: Sort) =
                preferredOrderByRestricted(prefKey, prefHandler, defaultOrderBy, accountSort)

        //returns null if the preferred Sort has null toOrderBy, otherwise the preferred Sort (with defaultOrderBy as secondary sort), otherwise the defaultOrderBy
        private fun preferredOrderByRestricted(prefKey: PrefKey?, prefHandler: PrefHandler, defaultSort: Sort, restrictedSet: Array<Sort>): String? {
            val defaultOrderBy = defaultSort.toOrderBy()
            prefHandler.getString(prefKey, null)?.let { pref ->
                (try {
                    valueOf(pref).takeIf { restrictedSet.contains(it) }
                } catch (e: IllegalArgumentException) { null } ?: defaultSort).toOrderBy().let { configured ->
                    return if (configured == null) null else {
                        configured + ", " + defaultOrderBy
                    }
                }
            }
            return defaultOrderBy
        }
    }
}