package org.totschnig.myexpenses.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.squareup.phrase.Phrase
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.util.Utils

class GdprDialogFragment : ComposeBaseDialogFragment() {

    private val baseActivity: BaseActivity
        get() = requireActivity() as BaseActivity

    fun onClick(personalizedConsent: Boolean?) {
        personalizedConsent?.also {
            baseActivity.gdprConsent(it)
        } ?: kotlin.run {
            baseActivity.gdprNoConsent()
        }
        dismiss()
    }

    @Composable
    override fun BuildContent() {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val adProviders = "Google"
            Text(
                Phrase.from(context, R.string.gdpr_consent_message)
                    .put(Utils.PLACEHOLDER_APP_NAME, stringResource(R.string.app_name))
                    .put("ad_provider", adProviders)
                    .format().toString()
            )
            Button(onClick = { onClick(true) }) {
                Text(stringResource(R.string.pref_ad_consent_title))
            }
            Button(onClick = { onClick(null) }) {
                Text(stringResource(R.string.gdpr_consent_button_no))
            }
            Button(onClick = { onClick(false) }) {
                Text(stringResource(R.string.ad_consent_non_personalized))
            }
        }
    }
}