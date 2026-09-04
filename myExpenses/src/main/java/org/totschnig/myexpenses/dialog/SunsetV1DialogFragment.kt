package org.totschnig.myexpenses.dialog

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.fragment.app.setFragmentResult
import com.squareup.phrase.Phrase
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.activity.MyExpensesV2
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.distrib.DistributionHelper

class SunsetV1DialogFragment : ComposeBaseDialogFragment(), DialogInterface.OnClickListener {

    private val isFromSettings: Boolean
        get() = arguments?.getBoolean(KEY_FROM_SETTINGS, false) == true

    private var dontShowAgain by mutableStateOf(false)

    @Composable
    override fun BuildContent() {
        SunsetV1Card()
    }

    @Composable
    private fun SunsetV1Card() {

        Column(Modifier.verticalScroll(rememberScrollState())) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.important_upgrade_information_heading),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.sunset_v1_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val context = requireContext()
                    val storeName = when {
                        DistributionHelper.isPlay -> "Play Store"
                        DistributionHelper.isAmazon -> "Amazon Appstore"
                        else -> "F-Droid"
                    }

                    val bodyText3 = Phrase.from(context, R.string.sunset_v1_message_3_body)
                        .put("app_name", stringResource(R.string.app_name))
                        .put("store", storeName)
                        .format()
                        .toString()

                    val items = listOf(
                        stringResource(R.string.sunset_v1_message_1_intro) to stringResource(R.string.migration_v2_opt_out_warning),
                        stringResource(R.string.sunset_v1_message_2_intro) to stringResource(R.string.sunset_v1_message_2_body),
                        stringResource(R.string.sunset_v1_message_3_intro) to bodyText3
                    )
                    items.forEach { (introText, bodyText) ->
                        val annotatedString = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("$introText ")
                            }
                            append(bodyText)
                        }
                        Text(
                            text = annotatedString,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            if (!isFromSettings) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            dontShowAgain = !dontShowAgain
                        }
                        .padding(top = 8.dp)
                ) {
                    Checkbox(
                        checked = dontShowAgain,
                        onCheckedChange = {
                            dontShowAgain = it
                            this@SunsetV1DialogFragment.dontShowAgain = it
                        }
                    )
                    Text(
                        text = stringResource(R.string.do_not_show_again),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    override fun initBuilder(): AlertDialog.Builder =
        super.initBuilder().apply {
            val listener = this@SunsetV1DialogFragment
            setPositiveButton(
                if (isFromSettings) android.R.string.cancel else R.string.switch_to_v2,
                listener
            )
            setNegativeButton(R.string.migration_v2_confirm_opt_out, listener)
            setNeutralButton(R.string.feedback, listener)
        }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        when (which) {
            AlertDialog.BUTTON_POSITIVE -> {
                if (!isFromSettings) {
                    prefHandler.mainScreenLegacy = false
                    prefHandler.putBoolean(PrefKey.SUNSET_V1_DISMISSED, false)
                    val intent = Intent(requireContext(), MyExpensesV2::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    requireActivity().finish()
                }
                dismiss()
            }

            AlertDialog.BUTTON_NEGATIVE -> {
                if (isFromSettings) {
                    prefHandler.mainScreenLegacy = true
                    setFragmentResult(
                        REQUEST_KEY,
                        Bundle().apply { putBoolean(RESULT_CONFIRMED, true) })
                } else if (dontShowAgain) {
                    prefHandler.putBoolean(PrefKey.SUNSET_V1_DISMISSED, true)
                }
                dismiss()
            }

            AlertDialog.BUTTON_NEUTRAL -> {
                (requireActivity() as BaseActivity).sendEmail(
                    recipient = getString(R.string.support_email),
                    subject = "[" + getString(R.string.app_name) + "] " +
                            getString(R.string.feedback) + " : " + getString(R.string.migration_v2_feedback_title),
                    body = ""
                )
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!isFromSettings && dontShowAgain) {
            prefHandler.putBoolean(PrefKey.SUNSET_V1_DISMISSED, true)
        }
    }

    companion object {
        const val REQUEST_KEY = "LEGACY_UI_REQUEST"
        const val RESULT_CONFIRMED = "confirmed"
        private const val KEY_FROM_SETTINGS = "from_settings"

        fun newInstance(fromSettings: Boolean = false) = SunsetV1DialogFragment().apply {
            arguments = Bundle().apply { putBoolean(KEY_FROM_SETTINGS, fromSettings) }
        }
    }
}
