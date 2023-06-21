package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageTemplates
import org.totschnig.myexpenses.compose.LocalDateFormatter
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.util.epoch2LocalDate
import org.totschnig.myexpenses.viewmodel.PlanInstanceInfo

class OrphanedTransactionDialog : ComposeBaseDialogFragment() {

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun BuildContent() {
        val transactionId = requireArguments().getLong(KEY_TRANSACTIONID)
        val relinkCandidate = requireArguments().getParcelable<PlanInstanceInfo?>(KEY_RELINK_TARGET)
        val host = requireActivity() as ManageTemplates
        Column(
            modifier = Modifier.padding(
                top = dialogPadding,
                start = dialogPadding,
                end = dialogPadding
            )
        ) {
            Text(stringResource(R.string.orphaned_transaction_info))
            if (relinkCandidate != null) {
                Text(
                    stringResource(
                        id = R.string.orphaned_transaction_relink,
                        LocalDateFormatter.current.format(
                            epoch2LocalDate(relinkCandidate.date!! / 1000)
                        )
                    ) + " " + stringResource(R.string.orphaned_transaction_align_date)
                )
            }
            FlowRow {
                TextButton(onClick = {
                    host.dispatchEditInstance(transactionId)
                    dismiss()
                }) {
                    Text(getString(R.string.menu_edit_plan_instance))
                }
                TextButton(onClick = {
                    host.dispatchDeleteInstance(transactionId)
                    dismiss()
                }) {
                    Text(getString(R.string.menu_delete))
                }
                relinkCandidate?.let {
                    TextButton(onClick = {
                        host.dispatchRelinkInstance(relinkCandidate, false)
                        dismiss()
                    }) {
                        Text(stringResource(id = R.string.button_relink_keep_date))
                    }
                    TextButton(onClick = {
                        host.dispatchRelinkInstance(relinkCandidate, true)
                        dismiss()
                    }) {
                        Text(stringResource(id = R.string.button_relink_update_date))
                    }
                }
            }
        }
    }

    companion object {
        private const val KEY_RELINK_TARGET = "relinkTarget"
        fun newInstance(transactionId: Long, relinkCandidate: PlanInstanceInfo?) =
            OrphanedTransactionDialog().apply {
                arguments = Bundle().apply {
                    putLong(KEY_TRANSACTIONID, transactionId)
                    putParcelable(KEY_RELINK_TARGET, relinkCandidate)
                }
            }
    }

}