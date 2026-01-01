package org.totschnig.myexpenses.model

import android.content.Context
import androidx.annotation.StringRes
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.viewmodel.data.FullAccount

interface AccountGroupingKey {
    val id: Any
    fun title(context: Context): String
    object Ungrouped: AccountGroupingKey {
        override val id = Unit
        override fun title(context: Context) = context.getString(R.string.grand_total)
    }
}

/**
 * grouping of accounts in account list
 */
enum class AccountGrouping(@param:StringRes val title: Int) {
    TYPE(R.string.type) {
        override fun getGroupKey(account: FullAccount)  = account.type
    },
    CURRENCY(R.string.currency) {
        override fun getGroupKey(account: FullAccount) = account.currencyUnit
    },
    FLAG(R.string.menu_flag) {
        override fun getGroupKey(account: FullAccount) = account.flag
    },

    NONE(R.string.grouping_none) {
        override fun getGroupKey(account: FullAccount) = AccountGroupingKey.Ungrouped
    }
    ;

    abstract fun getGroupKey(account: FullAccount): AccountGroupingKey
}