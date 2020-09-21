package org.totschnig.myexpenses.feature

import java.lang.Exception

interface FeatureManager {
    enum class Feature {
        OCR;
    }
    fun isFeatureInstalled(feature: Feature): Boolean
    fun requestFeature(feature: Feature, callback: Callback)
}

interface Callback {
    fun onAvailable()
    fun onAsyncStarted(feature: FeatureManager.Feature)
    fun onError(throwable: Throwable)
}