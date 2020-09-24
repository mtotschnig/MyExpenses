package org.totschnig.myexpenses.activity

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import icepick.State
import org.totschnig.myexpenses.activity.ExpenseEdit.Companion.KEY_OCR_RESULT
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.feature.OcrHost
import org.totschnig.myexpenses.feature.OcrResult
import org.totschnig.myexpenses.provider.DatabaseConstants
import java.io.File

abstract class BaseMyExpenses : LaunchActivity(), OcrHost {
    @JvmField
    @State
    var scanFile: File? = null
    @JvmField
    @State
    var accountId: Long = 0
    var currentCurrency: String? = null
    override fun processOcrResult(result: Result<OcrResult>) {
        result.onSuccess {
            startEdit(
                    createRowIntent().apply {
                        putExtra(KEY_OCR_RESULT, it)
                        putExtra(DatabaseConstants.KEY_PICTURE_URI, Uri.fromFile(scanFile))
                    }
            )
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * start ExpenseEdit Activity for a new transaction/transfer/split
     * Originally the form for transaction is rendered, user can change from spinner in toolbar
     */
    open fun createRowIntent() = Intent(this, ExpenseEdit::class.java).apply {
        putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
        //if we are called from an aggregate cursor, we also hand over the currency
        if (accountId < 0) {
            putExtra(DatabaseConstants.KEY_CURRENCY, currentCurrency)
            putExtra(ExpenseEdit.KEY_AUTOFILL_MAY_SET_ACCOUNT, true)
        } else {
            //if accountId is 0 ExpenseEdit will retrieve the first entry from the accounts table
            putExtra(DatabaseConstants.KEY_ACCOUNTID, accountId)
        }
    }

    open fun createRow() {
        startEdit(createRowIntent())
    }

    protected open fun startEdit(intent: Intent?) {
        startActivityForResult(intent, EDIT_REQUEST)
    }
}