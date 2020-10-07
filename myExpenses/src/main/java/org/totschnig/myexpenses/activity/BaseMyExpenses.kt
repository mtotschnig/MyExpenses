package org.totschnig.myexpenses.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.form.Hint
import eltos.simpledialogfragment.form.SimpleFormDialog
import eltos.simpledialogfragment.form.Spinner
import icepick.State
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit.Companion.KEY_OCR_RESULT
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.feature.OcrHost
import org.totschnig.myexpenses.feature.OcrResult
import org.totschnig.myexpenses.feature.OcrResultFlat
import org.totschnig.myexpenses.feature.Payee
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import java.io.File

const val DIALOG_TAG_OCR_DISAMBIGUATE = "DISAMBIGUATE"

abstract class BaseMyExpenses : LaunchActivity(), OcrHost, OnDialogResultListener {
    @JvmField
    @State
    var scanFile: File? = null

    @JvmField
    @State
    var accountId: Long = 0
    var currentCurrency: String? = null
    override fun processOcrResult(result: Result<OcrResult>) {
        result.onSuccess {
            if (it.isEmpty()) {
                Toast.makeText(this, getString(R.string.scan_result_no_data), Toast.LENGTH_LONG).show()
            } else if (it.needsDisambiguation()) {
                SimpleFormDialog.build()
                        .cancelable(false)
                        .autofocus(false)
                        .neg(android.R.string.cancel)
                        .extra(Bundle().apply {
                            putParcelable(KEY_OCR_RESULT, it)
                        })
                        .title(getString(R.string.scan_result_multiple_candidates_dialog_title))
                        .fields(
                                if (it.amountCandidates.isEmpty()) Hint.plain(getString(R.string.scan_result_no_amount)) else
                                    Spinner.plain(KEY_AMOUNT)
                                            .placeholder(R.string.amount)
                                            .items(*it.amountCandidates.toTypedArray())
                                            .preset(0),
                                if (it.dateCandidates.isEmpty()) Hint.plain(getString(R.string.scan_result_no_date)) else
                                    Spinner.plain(KEY_DATE)
                                            .placeholder(R.string.date)
                                            .items(*it.dateCandidates.map { pair ->
                                                (pair.second?.let { pair.first.atTime(pair.second) }
                                                        ?: pair.second).toString()
                                            }.toTypedArray())
                                            .preset(0),
                                if (it.payeeCandidates.isEmpty()) Hint.plain(getString(R.string.scan_result_no_payee)) else
                                    Spinner.plain(KEY_PAYEE_NAME)
                                            .placeholder(R.string.payee)
                                            .items(*it.payeeCandidates.map(Payee::name).toTypedArray())
                                            .preset(0),
                        )
                        .show(this, DIALOG_TAG_OCR_DISAMBIGUATE)
            } else {
                startEditFromOcrResult(it.selectCandidates())
            }
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

    fun createRow() {
        startEdit(createRowIntent())
    }

    protected fun startEdit(intent: Intent?) {
        startActivityForResult(intent, EDIT_REQUEST)
    }

    private fun startEditFromOcrResult(result: OcrResultFlat) {
        recordUsage(ContribFeature.OCR)
        startEdit(
                createRowIntent().apply {
                    putExtra(KEY_OCR_RESULT, result)
                    putExtra(DatabaseConstants.KEY_PICTURE_URI, Uri.fromFile(scanFile))
                }
        )
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (DIALOG_TAG_OCR_DISAMBIGUATE == dialogTag && which == OnDialogResultListener.BUTTON_POSITIVE) {
            startEditFromOcrResult(extras.getParcelable<OcrResult>(KEY_OCR_RESULT)!!.selectCandidates(
                    extras.getInt(KEY_AMOUNT), extras.getInt(KEY_DATE), extras.getInt(KEY_PAYEE_NAME)))
        }
        return false
    }
}