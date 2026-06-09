package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.totschnig.myexpenses.provider.KEY_CODE
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.viewmodel.DatabaseHandler.DeleteListener
import org.totschnig.myexpenses.viewmodel.DatabaseHandler.InsertListener
import javax.inject.Inject

class EditCurrencyViewModel(application: Application) : CurrencyViewModel(application) {
    @Inject
    lateinit var currencyFormatter: ICurrencyFormatter

    private val asyncDatabaseHandler: DatabaseHandler
    private var updateOperationsCount = 0
    private var updatedAccountsCount: Int? = null

    private val _updateComplete = MutableLiveData<Int?>()

    private val _insertComplete = MutableLiveData<Boolean?>()

    private val _deleteComplete = MutableLiveData<Boolean?>()

    val updateComplete: LiveData<Int?> = _updateComplete
    val insertComplete: LiveData<Boolean?> = _insertComplete
    val deleteComplete: LiveData<Boolean?> = _deleteComplete

    init {
        val contentResolver = application.contentResolver
        asyncDatabaseHandler = DatabaseHandler(contentResolver)
    }

    fun save(
        currency: String,
        symbol: String,
        fractionDigits: Int,
        label: String?,
        withUpdate: Boolean
    ) {
        val updateListener = DatabaseHandler.UpdateListener { token: Int, resultCount: Int ->
            updateOperationsCount--
            if (token == TOKEN_UPDATE_FRACTION_DIGITS) {
                updatedAccountsCount = resultCount
            }
            if (updateOperationsCount == 0) {
                currencyFormatter.invalidate(
                    getApplication<Application>().contentResolver,
                    currency
                )
                _updateComplete.postValue(updatedAccountsCount)
            }
        }
        currencyContext.storeCustomSymbol(currency, symbol)
        if (withUpdate) {
            updateOperationsCount++
            asyncDatabaseHandler.startUpdate(
                TOKEN_UPDATE_FRACTION_DIGITS, updateListener,
                TransactionProvider.CURRENCIES_URI.buildUpon()
                    .appendPath(TransactionProvider.URI_SEGMENT_CHANGE_FRACTION_DIGITS)
                    .appendPath(currency)
                    .appendPath(fractionDigits.toString())
                    .build(), null, null, null
            )
        } else {
            currencyContext.storeCustomFractionDigits(currency, fractionDigits)
        }
        if (label != null) {
            updateOperationsCount++
            val contentValues = ContentValues(1)
            contentValues.put(KEY_LABEL, label)
            asyncDatabaseHandler.startUpdate(
                TOKEN_UPDATE_LABEL, updateListener, buildItemUri(currency),
                contentValues, null, null
            )
        }
        if (updateOperationsCount == 0) {
            _updateComplete.postValue(null)
        }
    }


    fun newCurrency(code: String, symbol: String, fractionDigits: Int, label: String?) {
        val contentValues = ContentValues(2)
        contentValues.put(KEY_LABEL, label)
        contentValues.put(KEY_CODE, code)
        asyncDatabaseHandler.startInsert(
            TOKEN_INSERT_CURRENCY,
            InsertListener { _: Int, uri: Uri? ->
                val success = uri != null
                if (success) {
                    currencyContext.storeCustomSymbol(code, symbol)
                    currencyContext.storeCustomFractionDigits(code, fractionDigits)
                }
                _insertComplete.postValue(success)
            },
            TransactionProvider.CURRENCIES_URI,
            contentValues
        )
    }


    fun deleteCurrency(currency: String?) {
        asyncDatabaseHandler.startDelete(
            TOKEN_DELETE_CURRENCY,
            DeleteListener { _: Int, result: Int -> _deleteComplete.postValue(result == 1) },
            buildItemUri(currency),
            null,
            null
        )
    }

    private fun buildItemUri(currency: String?): Uri? {
        return TransactionProvider.CURRENCIES_URI.buildUpon().appendPath(currency).build()
    }

    companion object {
        private const val TOKEN_UPDATE_FRACTION_DIGITS = 1
        private const val TOKEN_UPDATE_LABEL = 2
        private const val TOKEN_INSERT_CURRENCY = 3
        private const val TOKEN_DELETE_CURRENCY = 4
    }
}
