package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.db2.FLAG_TRANSFER
import org.totschnig.myexpenses.db2.asCategoryType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.isHomeAggregate
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.uriBuilderForTransactionList
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_COMMITTED
import org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_SPLIT
import org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_SPLIT_PART
import org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_VOID
import org.totschnig.myexpenses.provider.DatabaseConstants.getAmountHomeEquivalent
import org.totschnig.myexpenses.provider.DbUtils
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
        val type: Boolean,
        val aggregateNeutral: Boolean = false,
        val withTransfers: Boolean = false,
        val icon: String? = null
    ) : Parcelable

    val sum: Flow<Long>
        get() = with(loadingInfo) {
            val (selection, selectionArgs) = selectionInfo
            contentResolver.observeQuery(
                transactionUri, arrayOf("sum($amountCalculation)"), selection, selectionArgs
            ).mapToOne {
                it.getLong(0)
            }
        }

    private val transactionUri
        get() = uriBuilderForTransactionList(
            loadingInfo.accountId,
            loadingInfo.currency.code,
            shortenComment = true,
            extended = false
        ).apply {
            if (loadingInfo.catId != 0L) {
                appendQueryParameter(KEY_CATID, loadingInfo.catId.toString())
            }
        }.build()

    val transactions: Flow<List<Transaction2>>
        get() = with(loadingInfo) {
            val (selection, selectionArgs) = selectionInfo
            contentResolver.observeQuery(
                transactionUri,
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
            if (catId == 0L) {
                selectionParts += WHERE_NOT_SPLIT
            }
            groupingClause?.takeIf { it.isNotEmpty() }?.let {
                selectionParts += it
                selectionArgs.addAll(groupingArgs.toTypedArray())
            }
            val types = buildList {
                add(type.asCategoryType)
                if(aggregateNeutral) {
                    add(FLAG_NEUTRAL)
                }
                if(withTransfers) {
                    add(FLAG_TRANSFER)
                }
            }
            val typeExpression = if(aggregateNeutral) KEY_TYPE else effectiveTypeExpression(DbUtils.typeWithFallBack(prefHandler))
            selectionParts += "$typeExpression IN (${types.joinToString()})"
            selectionParts.joinToString(" AND ") to selectionArgs.toTypedArray()
        }
}

