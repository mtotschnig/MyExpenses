package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.io.IOException

class OnBoardingUiViewModel(application: Application) : SyncViewModel(application) {
    fun setRenderer(compact: Boolean) {
        setBooleanPreference(PrefKey.UI_ITEM_RENDERER_LEGACY, compact)
    }

    fun setWithCategoryIcon(withCategoryIcon: Boolean) {
        setBooleanPreference(PrefKey.UI_ITEM_RENDERER_CATEGORY_ICON, withCategoryIcon)
    }

    private fun setBooleanPreference(prefKey: PrefKey, newValue: Boolean) {
        viewModelScope.launch(context = coroutineContext()) {
            try {
                dataStore.edit{ it[booleanPreferencesKey(prefHandler.getKey(prefKey))] = newValue }
            } catch (ex: IOException) {
                CrashHandler.report(ex)
            }
        }
    }
}