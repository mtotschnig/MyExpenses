package org.totschnig.myexpenses.model

import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LAST_USED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_KEY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USAGES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.util.enumValueOrNull

enum class Sort(val commandId: Int, val isDescending: Boolean = true) {
    USAGES(R.id.SORT_USAGES_COMMAND),
    LAST_USED(R.id.SORT_LAST_USED_COMMAND),
    AMOUNT(R.id.SORT_AMOUNT_COMMAND),
    TITLE(R.id.SORT_TITLE_COMMAND, false),
    LABEL(R.id.SORT_LABEL_COMMAND, false),
    CUSTOM(R.id.SORT_CUSTOM_COMMAND, false),
    NEXT_INSTANCE(R.id.SORT_NEXT_INSTANCE_COMMAND),
    ACCOUNT(R.id.SORT_ACCOUNT_COMMAND, false),
    ALLOCATED(0),
    SPENT(0),
    AVAILABLE(0),
    PAYEE_NAME(R.id.SORT_PAYEE_NAME_COMMAND),
    DEBT_SUM(R.id.SORT_DEBT_SUM_COMMAND);

    private fun toDatabaseColumn(collate: String) = when (this) {
        USAGES -> KEY_USAGES
        LAST_USED -> KEY_LAST_USED
        AMOUNT -> "abs($KEY_AMOUNT)"
        TITLE -> "$KEY_TITLE COLLATE $collate"
        LABEL -> "$KEY_LABEL COLLATE $collate"
        ACCOUNT -> "$KEY_ACCOUNT_LABEL COLLATE $collate"
        CUSTOM -> KEY_SORT_KEY
        PAYEE_NAME -> "$KEY_PAYEE_NAME COLLATE $collate"
        else -> null
    }

    fun toOrderBy(collate: String, isDescending: Boolean = this.isDescending, tableName: String? = null) =
        toDatabaseColumn(collate)?.let { column ->
            "${tableName?.let { "$it." } ?: ""}$column${if (isDescending) " DESC" else ""}"
    }

    fun toOrderByWithDefault(defaultSort: Sort, collate: String): String? {
        val orderBy = toOrderBy(collate)
        return if (orderBy == null || this == defaultSort) orderBy else
            orderBy + ", " + defaultSort.toOrderBy(collate)
    }

    companion object {
        private val templateSort = arrayOf(TITLE, USAGES, LAST_USED, AMOUNT, ACCOUNT)
        private val templateWithPlansSort = arrayOf(TITLE, USAGES, LAST_USED, AMOUNT, ACCOUNT, NEXT_INSTANCE)
        val accountSort = arrayOf(LABEL, USAGES, LAST_USED, CUSTOM)
        val accountSortLabels = listOf(R.string.label, R.string.pref_sort_order_usages, R.string.pref_sort_order_last_used, R.string.pref_sort_order_custom)

        @JvmStatic
        fun fromCommandId(id: Int): Sort? {
            for (sort in entries) {
                if (sort.commandId == id) return sort
            }
            return null
        }

        fun preferredOrderByForTemplates(
            prefHandler: PrefHandler,
            defaultSort: Sort,
            collate: String = "NOCASE"
        ) =
            preferredOrderByRestricted(
                PrefKey.SORT_ORDER_TEMPLATES,
                prefHandler,
                defaultSort,
                templateSort,
                collate
            )

        fun preferredOrderByForTemplatesWithPlans(
            prefHandler: PrefHandler,
            defaultSort: Sort,
            collate: String = "NOCASE"
        ) =
            preferredOrderByRestricted(
                PrefKey.SORT_ORDER_TEMPLATES,
                prefHandler,
                defaultSort,
                templateWithPlansSort,
                collate
            )

        fun preferredOrderByForAccounts(
            prefKey: PrefKey,
            prefHandler: PrefHandler,
            defaultSort: Sort,
            collate: String,
            tableName: String?
        ) =
            preferredOrderByRestricted(prefKey, prefHandler, defaultSort, accountSort, collate, tableName)

        //returns null if the preferred Sort has null toOrderBy, otherwise the preferred Sort (with defaultOrderBy as secondary sort), otherwise the defaultOrderBy
        private fun preferredOrderByRestricted(
            prefKey: PrefKey,
            prefHandler: PrefHandler,
            defaultSort: Sort,
            restrictedSet: Array<Sort>,
            collate: String,
            tableName: String? = null
        ): String? {
            if (!restrictedSet.contains(defaultSort)) throw java.lang.IllegalArgumentException(
                "%s is not part of %s".format(defaultSort, restrictedSet)
            )
            val configuredOrDefault = enumValueOrNull<Sort>(
                prefHandler.requireString(
                    prefKey,
                    defaultSort.name
                )
            )?.takeIf {
                restrictedSet.contains(it)
            } ?: defaultSort
            val orderBy = configuredOrDefault.toOrderBy(collate, tableName = tableName)
            return if (orderBy == null || configuredOrDefault == defaultSort) orderBy else
                orderBy + ", " + defaultSort.toOrderBy(collate, tableName = tableName)
        }
    }
}
