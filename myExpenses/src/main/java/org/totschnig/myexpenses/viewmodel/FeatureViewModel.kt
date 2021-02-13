package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.feature.Callback
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.feature.FeatureManager
import javax.inject.Inject

class FeatureViewModel (application: Application) : AndroidViewModel(application) {
    @Inject
    lateinit var featureManager: FeatureManager
    init {
        (application as MyApplication).appComponent.inject(this)
        featureManager.registerCallback(object : Callback {
            override fun onFeatureAvailable(moduleNames: List<String>) {
                featureState.postValue(FeatureState.Available(moduleNames))
            }

            override fun onAsyncStartedFeature(feature: Feature) {
                featureState.postValue(FeatureState.Loading(feature))
            }

            override fun onError(throwable: Throwable) {
                featureState.postValue(FeatureState.Error(throwable))
            }

        })
    }

    sealed class FeatureState<out T> {
        data class Error(val throwable: Throwable) : FeatureState<Throwable>()
        data class Loading(val feature: Feature): FeatureState<Feature>()
        data class Available(val modules: List<String>): FeatureState<List<String>>()
    }

    private val featureState = MutableLiveData<FeatureState<*>>()

    fun getFeatureState(): LiveData<FeatureState<*>> {
        return featureState
    }

    fun isFeatureAvailable(context: Context, feature: Feature) = featureManager.isFeatureInstalled(feature, context)

    fun requestFeature(activity: BaseActivity, feature: Feature) = featureManager.requestFeature(feature, activity)
}