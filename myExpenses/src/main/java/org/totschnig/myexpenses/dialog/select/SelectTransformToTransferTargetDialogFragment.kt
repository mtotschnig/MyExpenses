package org.totschnig.myexpenses.dialog.select

import android.annotation.SuppressLint
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.viewmodel.data.Transaction2

class SelectTransformToTransferTargetDialogFragment : SelectSingleAccountDialogFragment() {
    override fun AlertDialog.Builder.configurePositiveButton() {
        setPositiveButton(submitLabel("?"), null)
    }

    @SuppressLint("SetTextI18n")
    override fun updateSubmitButton(button: Button, dataHolder: DataHolder) {
        super.updateSubmitButton(button, dataHolder)
        button.text = submitLabel(dataHolder.label)
    }

    private val transactionId
        get() = requireArguments().getLong(KEY_TRANSACTIONID)

    private val isIncome
        get() = requireArguments().getBoolean(KEY_IS_INCOME)

    private fun submitLabel(target: String) = getString(
        if (isIncome) R.string.transfer_from_account else R.string.transfer_to_account
    ) + " " + target

    override fun buildExtras() = super.buildExtras()?.apply {
        putLong(KEY_TRANSACTIONID, transactionId)
        putBoolean(KEY_IS_INCOME, isIncome)
    }

    companion object {
        const val TRANSFORM_TO_TRANSFER_REQUEST = "transformToTransfer"
        const val KEY_IS_INCOME = "isIncome"
        fun newInstance(transaction: Transaction2) =
            SelectTransformToTransferTargetDialogFragment().apply {
                arguments =
                    buildArguments(
                        R.string.menu_transform_to_transfer, 0,
                        TRANSFORM_TO_TRANSFER_REQUEST, listOf(transaction.accountId)
                    ).apply {
                        putLong(KEY_TRANSACTIONID, transaction.id)
                        putBoolean(KEY_IS_INCOME, transaction.displayAmount.amountMinor > 0)
                    }
            }
    }
}