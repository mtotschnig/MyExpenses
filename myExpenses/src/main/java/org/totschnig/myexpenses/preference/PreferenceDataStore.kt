package org.totschnig.myexpenses.preference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DYNAMIC
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.io.IOException
import javax.inject.Inject

// datastore/datastore-sampleapp/src/main/java/com/example/datastoresampleapp/SettingsFragment.kt
class PreferenceDataStore @Inject constructor(private val dataStore: DataStore<Preferences>) {

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun handleToggle(preference: TwoStatePreference) {
        val prefKey = booleanPreferencesKey(preference.key)
        val value = dataStore.data.first()[prefKey]
            ?: preference.isChecked //this gives us defaultValue from XML if no value is persisted yet

        with(preference) {

            isChecked = value
            isEnabled = true

            changeFlow.flatMapLatest { (_: Preference?, newValue: Any?) ->
                val checked = newValue as Boolean

                isEnabled = false
                isChecked = checked

                try {
                    dataStore.edit { it[prefKey] = checked }
                } catch (ex: IOException) {
                    CrashHandler.report(ex)
                }
                dataStore.data
            }.collect {
                isChecked = it[prefKey] == true
                isEnabled = true
            }

        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun handleList(
        preference: ListPreference,
        onChanged: ((String) -> Unit)? = null
    ) {
        val prefKey = stringPreferencesKey(preference.key)
        val value = dataStore.data.first()[prefKey]
            ?: preference.value ////this gives us defaultValue from XML if no value is persisted yet

        with(preference) {

            setValue(value)
            isEnabled = true

            changeFlow.flatMapLatest { (_: Preference?, newValue: Any?) ->
                val checked = newValue as String

                isEnabled = false
                setValue(value)

                try {
                    dataStore.edit {
                        it[prefKey] = checked
                        onChanged?.invoke(checked)
                    }
                } catch (ex: IOException) {
                    CrashHandler.report(ex)
                }
                dataStore.data
            }.collect {
                setValue(it[prefKey])
                isEnabled = true
            }

        }
    }
}

@ExperimentalCoroutinesApi
private val Preference.changeFlow: Flow<Pair<Preference?, Any?>>
    get() = callbackFlow {
        this@changeFlow.setOnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
            this@callbackFlow.launch {
                send(Pair(preference, newValue))
            }
            false // Do not update the state of the toggle.
        }

        awaitClose { this@changeFlow.onPreferenceChangeListener = null }
    }

const val DYNAMIC_EXCHANGE_RATES_DEFAULT_KEY = "dynamic_exchange_rates_default"

val dynamicExchangeRatesDefaultKey = stringPreferencesKey(DYNAMIC_EXCHANGE_RATES_DEFAULT_KEY)

/**
 * 1 if all accounts should have dynamic exchange rates,
 * 0 if none should have dynamic exchange rates,
 * KEY_DYNAMIC if dynamic exchange rates should be set per account
 */
val DataStore<Preferences>.dynamicExchangeRates: Flow<String>
    get() = data.map { preferences ->
        when(preferences[dynamicExchangeRatesDefaultKey]) {
            "DYNAMIC" -> "1"
            "STATIC" -> "0"
            else -> KEY_DYNAMIC
        }
    }

val DataStore<Preferences>.dynamicExchangeRatesPerAccount: Flow<Boolean>
    get() = dynamicExchangeRates.map { it == KEY_DYNAMIC }