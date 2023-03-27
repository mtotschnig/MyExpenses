package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.feature.Callback
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.viewmodel.data.Event
import javax.inject.Inject

class FeatureViewModel(application: Application) : AndroidViewModel(application) {
    @Inject
    lateinit var featureManager: FeatureManager

    private fun postAsEvent(state: FeatureState) {
        featureState.postValue(Event(state))
    }

    fun registerCallback() {
        featureManager.registerCallback(object : Callback {
            override fun onFeatureAvailable(moduleNames: List<String>) {
                postAsEvent(FeatureState.FeatureAvailable(moduleNames))
            }

            override fun onAsyncStartedFeature(feature: Feature) {
                postAsEvent(FeatureState.FeatureLoading(feature))
            }

            override fun onAsyncStartedLanguage(displayLanguage: String) {
                postAsEvent(FeatureState.LanguageLoading(displayLanguage))
            }

            override fun onError(throwable: Throwable) {
                postAsEvent(FeatureState.Error(throwable))
            }

            override fun onLanguageAvailable(language: String) {
                postAsEvent(FeatureState.LanguageAvailable(language))
            }

        })
    }

    fun unregisterCallback() {
        featureManager.unregister()
    }

    sealed class FeatureState {
        class Error(val throwable: Throwable) : FeatureState()
        class FeatureLoading(val feature: Feature) : FeatureState()
        class LanguageLoading(val language: String) : FeatureState()
        class FeatureAvailable(val modules: List<String>) : FeatureState()
        class LanguageAvailable(val language: String) : FeatureState()
    }

    private val featureState = MutableLiveData<Event<FeatureState>>()

    fun getFeatureState(): LiveData<Event<FeatureState>> {
        return featureState
    }

    fun isFeatureAvailable(context: Context, feature: Feature) =
        featureManager.isFeatureInstalled(feature, context)

    fun requestFeature(activity: BaseActivity, feature: Feature) =
        featureManager.requestFeature(feature, activity)

    fun requireFeature(activity: BaseActivity, feature: Feature) {
        if (!isFeatureAvailable(activity, feature)) requestFeature(activity, feature)
    }
}