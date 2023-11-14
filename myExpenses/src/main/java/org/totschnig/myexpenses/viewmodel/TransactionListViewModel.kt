package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.db2.FLAG_TRANSFER
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.isAggregate
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.isHomeAggregate
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.uriBuilderForTransactionList
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.effectiveTypeExpression
import org.totschnig.myexpenses.viewmodel.data.Transaction2

const val KEY_LOADING_INFO = "loadingInfo"

class TransactionListViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : ContentResolvingAndroidViewModel(application) {

    val loadingInfo
        get() = savedStateHandle.get<LoadingInfo>(KEY_LOADING_INFO)!!

    @Parcelize
    data class LoadingInfo(
        val accountId: Long,
        val currency: CurrencyUnit,
        val catId: Long = 0,
        val grouping: Grouping?,
        val groupingClause: String?,
        val groupingArgs: List<String> = emptyList(),
        val label: String?,
        val type: Byte,
        val withTransfers: Boolean = true,
        val icon: String? = null
    ) : Parcelable

    val sum: Flow<Long>
        get() = with(loadingInfo) {
            val (selection, selectionArgs) = selectionInfo
            contentResolver.observeQuery(
                TransactionProvider.TRANSACTIONS_URI.let {
                    if (catId != 0L) {
                        it.buildUpon().appendQueryParameter(KEY_CATID, catId.toString()).build()
                    } else it
                }, arrayOf("sum($amountCalculation)"), selection, selectionArgs
            ).mapToOne {
                it.getLong(0)
            }
        }


    val transactions: Flow<List<Transaction2>>
        get() = with(loadingInfo) {
            val (selection, selectionArgs) = selectionInfo
            contentResolver.observeQuery(
                uriBuilderForTransactionList(shortenComment = true, extended = false).apply {
                    if (catId != 0L) {
                        appendQueryParameter(KEY_CATID, catId.toString())
                    }
                }.build(),
                Transaction2.projection(
                    accountId,
                    Grouping.NONE,
                    homeCurrencyProvider.homeCurrencyString,
                    prefHandler,
                    extended = false
                ),
                selection,
                selectionArgs
            ).mapToList {
                Transaction2.fromCursor(
                    it,
                    currency
                )
            }
        }

    private val amountCalculation: String
        get() = if (isHomeAggregate(loadingInfo.accountId))
            getAmountHomeEquivalent(VIEW_COMMITTED, homeCurrencyProvider.homeCurrencyString)
        else KEY_AMOUNT

    private val selectionInfo: Pair<String, Array<String>>
        get() = with(loadingInfo) {
            val selectionParts = mutableListOf<String>()
            val selectionArgs = mutableListOf<String>()
            selectionParts += WHERE_NOT_VOID
            when {
                isHomeAggregate(accountId) -> {}
                isAggregate(accountId) -> {
                    selectionParts += buildString {
                        append(KEY_ACCOUNTID)
                        append(" IN (SELECT ")
                        append(KEY_ROWID)
                        append(" FROM ")
                        append(TABLE_ACCOUNTS)
                        append(" WHERE ")
                        append(KEY_CURRENCY)
                        append(" = ? AND ")
                        append(KEY_EXCLUDE_FROM_TOTALS)
                        append(" = 0)")
                    }
                    selectionArgs += currency.code
                }

                else -> {
                    selectionParts += "$KEY_ACCOUNTID = ?"
                    selectionArgs += accountId.toString()
                }
            }
            if (catId == 0L) {
                selectionParts += WHERE_NOT_SPLIT_PART
            }
            groupingClause?.takeIf { it.isNotEmpty() }?.let {
                selectionParts += it
                selectionArgs.addAll(groupingArgs.toTypedArray())
            }
            if (!(type == FLAG_NEUTRAL && withTransfers)) {
                val types = buildList {
                    if(type != FLAG_EXPENSE) {
                        add(FLAG_INCOME)
                    }
                    if(type != FLAG_INCOME) {
                        add(FLAG_EXPENSE)
                    }
                    if(withTransfers) {
                        add(FLAG_TRANSFER)
                    }
                }
                selectionParts += "${effectiveTypeExpression(DbUtils.typeWithFallBack(prefHandler))} IN (${types.joinToString()})"
            }

            selectionParts.joinToString(" AND ") to selectionArgs.toTypedArray()
        }
}

