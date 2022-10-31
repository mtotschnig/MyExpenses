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
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.io.IOException
import javax.inject.Inject

// datastore/datastore-sampleapp/src/main/java/com/example/datastoresampleapp/SettingsFragment.kt
class PreferenceDataStore @Inject constructor(private val dataStore: DataStore<Preferences>) {

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun handleToggle(preference: TwoStatePreference) {
        val prefKey = booleanPreferencesKey(preference.key)
        val value = dataStore.data.first()[prefKey] ?: preference.isChecked //this gives us defaultValue from XML if no value is persisted yet

        with(preference) {

            isChecked = value
            isEnabled = true

            changeFlow.flatMapLatest { (_: Preference?, newValue: Any?) ->
                val checked = newValue as Boolean

                isEnabled = false
                isChecked = checked

                try {
                    dataStore.edit{ it[prefKey] = checked }
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
    suspend fun handleList(preference: ListPreference) {
        val prefKey = stringPreferencesKey(preference.key)
        val value = dataStore.data.first()[prefKey] ?: preference.value ////this gives us defaultValue from XML if no value is persisted yet

        with(preference) {

            setValue(value)
            isEnabled = true

            changeFlow.flatMapLatest { (_: Preference?, newValue: Any?) ->
                val checked = newValue as String

                isEnabled = false
                setValue(value)

                try {
                    dataStore.edit{ it[prefKey] = checked }
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