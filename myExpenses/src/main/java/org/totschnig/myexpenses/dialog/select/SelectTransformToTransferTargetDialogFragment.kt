package org.totschnig.myexpenses.dialog.select

import android.annotation.SuppressLint
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.viewmodel.data.Transaction2

class SelectTransformToTransferTargetDialogFragment : SelectSingleAccountDialogFragment() {
    override fun AlertDialog.Builder.configurePositiveButton() {
        setPositiveButton(getString(R.string.transfer_to_account) + " ?", null)
    }

    @SuppressLint("SetTextI18n")
    override fun updateSubmitButton(button: Button, dataHolder: DataHolder) {
        super.updateSubmitButton(button, dataHolder)
        button.text = getString(R.string.transfer_to_account) + " " + dataHolder.label
    }

    override fun buildExtras() = super.buildExtras()?.apply {
        putLong(KEY_TRANSACTIONID, requireArguments().getLong(KEY_TRANSACTIONID))
    }

    companion object {
        const val TRANSFORM_TO_TRANSFER_REQUEST = "transformToTransfer"
        fun newInstance(transaction: Transaction2) =
            SelectTransformToTransferTargetDialogFragment().apply {
                arguments =
                    buildArguments(R.string.menu_transform_to_transfer, 0,
                        TRANSFORM_TO_TRANSFER_REQUEST, listOf(transaction.accountId)).apply {
                            putLong(KEY_TRANSACTIONID, transaction.id)
                    }
            }
    }
}