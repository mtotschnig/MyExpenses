package org.totschnig.myexpenses.model

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.saveable.Saver
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model2.AccountWithGroupingKey
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.FullAccount

interface AccountGroupingKey {
    val id: Any
    fun title(context: Context): String

    object Ungrouped : AccountGroupingKey {
        override val id = Unit
        override fun title(context: Context) = context.getString(R.string.grand_total)
    }
}

const val KEY_ACCOUNT_GROUPING = "ACCOUNT_GROUPING"
const val KEY_ACCOUNT_GROUPING_GROUP = "ACCOUNT_GROUPING_GROUP"

/**
 * grouping of accounts in account list
 */

sealed class AccountGrouping<T : AccountGroupingKey>(
    @param:StringRes val title: Int,
    val comparator: Comparator<T>,
) {

    val ordinal: Int
        get() = ALL_VALUES.indexOf(this)

    val name: String
        get() = when(this) {
            CURRENCY -> "CURRENCY"
            FLAG -> "FLAG"
            NONE -> "NONE"
            TYPE -> "TYPE"
        }

    abstract fun getGroupKey(account: AccountWithGroupingKey): T

    open fun sortedGroupKeys(accounts: List<FullAccount>): List<T> = accounts
        .map { getGroupKey(it) }
        .distinct()
        .sortedWith(comparator)

    data object TYPE : AccountGrouping<AccountType>(
        title = R.string.type,
        comparator = compareByDescending(AccountType::sortKey)
    ) {
        override fun getGroupKey(account: AccountWithGroupingKey) = account.type
    }

    data object CURRENCY : AccountGrouping<CurrencyUnit>(
        title = R.string.currency,
        comparator = compareBy(CurrencyUnit::code)
    ) {
        override fun getGroupKey(account: AccountWithGroupingKey) = account.currencyUnit
    }

    data object FLAG : AccountGrouping<AccountFlag>(
        title = R.string.menu_flag,
        comparator = compareByDescending(AccountFlag::sortKey)
    ) {
        override fun getGroupKey(account: AccountWithGroupingKey) = account.flag
    }

    data object NONE : AccountGrouping<AccountGroupingKey.Ungrouped>(
        title = R.string.grouping_none,
        comparator = compareBy { 0 }
    ) {
        override fun getGroupKey(account: AccountWithGroupingKey) = AccountGroupingKey.Ungrouped
        override fun sortedGroupKeys(accounts: List<FullAccount>): List<AccountGroupingKey.Ungrouped> =
            emptyList()
    }

    companion object {
        val DEFAULT = TYPE
        val ALL_VALUES by lazy { arrayOf(TYPE, CURRENCY, FLAG, NONE) }

        fun valueOf(value: String): AccountGrouping<*> {
            return when (value) {
                "TYPE" -> TYPE
                "CURRENCY" -> CURRENCY
                "FLAG" -> FLAG
                "NONE" -> NONE
                else -> {
                    CrashHandler.report(IllegalArgumentException("No AccountGrouping $value"))
                    DEFAULT
                }
            }
        }

        val Saver: Saver<AccountGrouping<*>, *> = Saver(
            save = { value -> value.name },
            restore = { name -> valueOf(name) }
        )
    }
}