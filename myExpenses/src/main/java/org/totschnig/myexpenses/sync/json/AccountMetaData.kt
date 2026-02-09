package org.totschnig.myexpenses.sync.json

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.sort.SortDirection
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.KEY_DATE

@Serializable
@Parcelize
data class AccountMetaData(
    val label: String,
    val currency: String,
    val color: Int,
    val uuid: String,
    val openingBalance: Long,
    val description: String,
    val type: String,
    val exchangeRate: Double? = null,
    val exchangeRateOtherCurrency: String? = null,
    val excludeFromTotals: Boolean = false,
    val criterion: Long = 0L
) : Parcelable {

    override fun toString(): String {
        return "$label ($currency)"
    }

    fun toAccount(homeCurrency: String, syncAccount: String) = Account(
        0L,
        label,
        description,
        openingBalance,
        currency,
        AccountType.withName(type),
        AccountFlag.DEFAULT,
        color,
        criterion,
        syncAccount,
        false,
        uuid,
        false,
        KEY_DATE,
        SortDirection.DESC,
        if (homeCurrency != exchangeRateOtherCurrency) 1.0 else exchangeRate ?: 1.0,
        Grouping.NONE,
        null,
        false
    )

    companion object {
        fun from(account: Account, homeCurrency: String?): AccountMetaData {
            val accountCurrency = account.currency
            return AccountMetaData(
                label = account.label,
                currency = accountCurrency,
                color = account.color,
                uuid = account.uuid!!,
                description = account.description,
                openingBalance = account.openingBalance,
                type = account.type.name,
                excludeFromTotals = account.excludeFromTotals,
                criterion = account.criterion ?: 0L,
                exchangeRate = if (homeCurrency != null && homeCurrency != accountCurrency) account.exchangeRate else null,
                exchangeRateOtherCurrency = if (homeCurrency != null && homeCurrency != accountCurrency) homeCurrency else null
            )
        }
    }
}
