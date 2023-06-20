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
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageTemplates
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.util.epoch2LocalDate
import org.totschnig.myexpenses.viewmodel.PlanInstanceInfo
import java.time.LocalDate

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
            Text("A transaction is linked to the plan for this date, but no event instance exists at this date")
            if (relinkCandidate != null) {
                Text("The transaction can be relinked with the event instance on ${epoch2LocalDate(relinkCandidate.date!! / 1000)}. Optionally the date stored with the transaction can also be aligned with the instance date.")
            }
            FlowRow {
                TextButton(onClick = {
                    host.dispatchEditInstance(transactionId)
                    dismiss()
                }) {
                    Text(getString(R.string.menu_edit))
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
                        Text("Relink, but keep date")
                    }
                    TextButton(onClick = {
                        host.dispatchRelinkInstance(relinkCandidate, true)
                        dismiss()
                    }) {
                        Text("Relink and update date")
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