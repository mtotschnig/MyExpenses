package org.totschnig.myexpenses.next.data.merchants

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Whether the user agreed that the logos of [Merchants] are loaded from the internet. Off until
 * then, since the icon service learns the domains of the merchants the user pays.
 */
class MerchantLogoConsent(private val dataStore: DataStore<Preferences>) {

    val isGiven: Flow<Boolean> = dataStore.data.map { it[KEY_CONSENT] == true }.distinctUntilChanged()

    suspend fun set(consent: Boolean) {
        dataStore.edit { it[KEY_CONSENT] = consent }
    }

    private companion object {
        val KEY_CONSENT = booleanPreferencesKey("next_merchant_logos_consent")
    }
}
