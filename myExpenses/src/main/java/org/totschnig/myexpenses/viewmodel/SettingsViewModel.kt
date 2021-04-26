package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.totschnig.myexpenses.provider.TransactionProvider

class SettingsViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    private val hasStaleImagesLiveData by lazy {
        val liveData = MutableLiveData<Boolean>()
        disposable = briteContentResolver.createQuery(TransactionProvider.STALE_IMAGES_URI,
                arrayOf("count(*)"), null, null, null, true)
                .mapToOne { cursor -> cursor.getInt(0) > 0 }
                .subscribe { liveData.postValue(it) }
        return@lazy liveData
    }
    fun hasStaleImages(): LiveData<Boolean> = hasStaleImagesLiveData
}