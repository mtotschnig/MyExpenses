package org.totschnig.myexpenses.next.merchants

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.settings.AreaSettings
import org.totschnig.myexpenses.next.settings.InsightSetting
import org.totschnig.myexpenses.next.settings.SettingControl
import org.totschnig.myexpenses.next.settings.SettingKind
import org.totschnig.myexpenses.next.settings.SettingsGroup
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.totschnig.myexpenses.next.NextFeature
import org.totschnig.myexpenses.next.RowDecorations
import org.totschnig.myexpenses.next.core.ui.merchants.LocalPayeeImages
import org.totschnig.myexpenses.next.data.merchants.MerchantLogoConsent
import org.totschnig.myexpenses.next.data.merchants.MerchantLogoStore
import org.totschnig.myexpenses.next.data.merchants.Payee
import org.totschnig.myexpenses.next.data.merchants.PayeeDomainStore
import org.totschnig.myexpenses.next.data.merchants.PayeeIconStore
import org.totschnig.myexpenses.next.feature.merchants.MerchantLogos
import org.totschnig.myexpenses.next.feature.merchants.PayeeImageDialog
import org.totschnig.myexpenses.next.viewModelsOf

/** Icons and logos of payees, chosen by the user or loaded with their consent */
class MerchantsFeature(activity: ComponentActivity) : NextFeature {
    val logos by activity.viewModelsOf {
        val dataStore = activity.injector.preferencesDataStore()
        MerchantLogos(
            activity.contentResolver.rawPayees(),
            MerchantLogoConsent(dataStore),
            PayeeDomainStore(dataStore),
            PayeeIconStore(dataStore),
            MerchantLogoStore(activity.noBackupFilesDir.resolve("merchant-logos"))
        )
    }

    @Composable
    override fun rowDecorations(): RowDecorations {
        val images by logos.images.collectAsStateWithLifecycle()
        return RowDecorations(
            payeeImage = images::get,
            payeeImageDialog = { payee, transaction, onDismiss ->
                PayeeImageDialog(logos, Payee(payee.id, payee.displayName), transaction?.categoryPath, transaction?.icon, onDismiss)
            }
        )
    }

    /** Contracts and other screens show the icons and logos of payees too */
    @Composable
    override fun provided(): List<ProvidedValue<*>> {
        val images by logos.images.collectAsStateWithLifecycle()
        return listOf(LocalPayeeImages provides images::get)
    }
}

/** Whether logos of payees are loaded, in the settings */
class MerchantSettings(private val activity: ComponentActivity) : AreaSettings {
    private val logoConsent = consent(activity)

    @Composable
    override fun control(setting: InsightSetting): SettingControl? {
        if (setting != LOGOS) return null
        return SettingControl.Switch(
            checked = logoConsent.isGiven.collectAsState(false).value,
            summary = stringResource(R.string.next_settings_merchant_logos_summary),
            isEnabled = true,
            onChange = { activity.lifecycleScope.launch { logoConsent.set(it) } }
        )
    }

    companion object : AreaSettings.Factory {
        val LOGOS = InsightSetting(
            "MERCHANT_LOGOS", SettingsGroup.LISTS, SettingKind.SWITCH,
            R.string.next_settings_merchant_logos, R.string.next_settings_merchant_logos_summary
        )

        override val settings = listOf(LOGOS)

        override fun create(activity: ComponentActivity) = MerchantSettings(activity)

        /** Whether the user agreed to load logos of payees, also asked when the app is started first */
        fun consent(context: Context) = MerchantLogoConsent(context.injector.preferencesDataStore())
    }
}
