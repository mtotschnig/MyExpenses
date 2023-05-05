package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.os.Parcelable
import android.text.TextUtils
import androidx.lifecycle.SavedStateHandle
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.uriForTransactionList
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.isAggregate
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.isHomeAggregate
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_EXTENDED
import org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_SPLIT_PART
import org.totschnig.myexpenses.provider.DatabaseConstants.getAmountHomeEquivalent
import org.totschnig.myexpenses.provider.categoryTreeSelect
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.viewmodel.data.Transaction2

private const val KEY_LOADING_INFO = "loadingInfo"

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
        val catId: Long,
        val grouping: Grouping?,
        val groupingClause: String,
        val groupingArgs: List<String>,
        val label: String?,
        val type: Int,
        val withTransfers: Boolean,
        val icon: String? = null
    ) : Parcelable

    fun loadTransactions(): Flow<List<Transaction2>> = with(loadingInfo) {
        val (selection, selectionArgs) = selectionInfo
        contentResolver.observeQuery(
            uriForTransactionList(shortenComment = true, extended = false),
            Transaction2.projection(
                accountId,
                Grouping.NONE,
                homeCurrencyProvider.homeCurrencyString,
                extended = false
            ),
            selection,
            selectionArgs
        ).mapToList {
            Transaction2.fromCursor(
                getApplication(),
                it,
                currencyContext,
                homeCurrencyProvider.homeCurrencyUnit,
                currency
            )
        }
    }

    private val amountCalculation: String
        get() = if (isHomeAggregate(loadingInfo.accountId))
            getAmountHomeEquivalent(VIEW_EXTENDED, homeCurrencyProvider.homeCurrencyString)
        else KEY_AMOUNT

    private val selectionInfo: Pair<String, Array<String>>
        get() = with(loadingInfo) {
            val selectionParts = mutableListOf<String>()
            val selectionArgs = mutableListOf<String>()
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
            } else {
                selectionParts += buildString {
                    append(KEY_CATID)
                    append(" IN (")
                    append(
                        categoryTreeSelect(
                            projection = arrayOf(KEY_ROWID),
                            rootExpression = WhereFilter.Operation.IN.getOp(1)
                        )
                    )
                    append(")")
                }
                selectionArgs += catId.toString()
            }
            if (!TextUtils.isEmpty(groupingClause)) {
                selectionParts += groupingClause
                selectionArgs.addAll(groupingArgs.toTypedArray())
            }
            if (type != 0) {
                selectionParts += KEY_AMOUNT + (if (type == -1) "<" else ">") + "0"
            }
            if (!withTransfers) {
                selectionParts += "$KEY_TRANSFER_PEER is null"
            }
            selectionParts.joinToString(" AND ") to selectionArgs.toTypedArray()
        }

    /*    fun onCreateLoader(id: Int, arg1: Bundle?): Loader<Cursor> {

            when (id) {
                TransactionListDialogFragment.TRANSACTION_CURSOR -> return CursorLoader(
                    requireActivity(),
                    mAccount.extendedUriForTransactionList(type != 0, true),
                    mAccount.extendedProjectionForTransactionList,
                    selection,
                    selectionArgs,
                    null
                )

                TransactionListDialogFragment.SUM_CURSOR -> return CursorLoader(
                    requireActivity(),
                    Transaction.EXTENDED_URI, arrayOf("sum($amountCalculation)"), selection,
                    selectionArgs, null
                )
            }
            throw IllegalArgumentException()
        }*/
}

