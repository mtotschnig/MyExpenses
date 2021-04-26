package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.exception.ExternalStorageNotAvailableException
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.io.FileUtils
import java.io.IOException

class SettingsViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    private val _appDirInfo: MutableLiveData<Result<Pair<String, Boolean>>> = MutableLiveData()
    val appDirInfo: LiveData<Result<Pair<String, Boolean>>> = _appDirInfo
    val hasStaleImages: LiveData<Boolean> by lazy {
        val liveData = MutableLiveData<Boolean>()
        disposable = briteContentResolver.createQuery(TransactionProvider.STALE_IMAGES_URI,
                arrayOf("count(*)"), null, null, null, true)
                .mapToOne { cursor -> cursor.getInt(0) > 0 }
                .subscribe { liveData.postValue(it) }
        return@lazy liveData
    }

    fun loadAppDirInfo() {
        viewModelScope.launch(context = coroutineContext()) {
            if (AppDirHelper.isExternalStorageAvailable()) {
                AppDirHelper.getAppDir(getApplication())?.let {
                    _appDirInfo.postValue(Result.success(Pair(FileUtils.getPath(getApplication(), it.uri),AppDirHelper.isWritableDirectory(it))))
                } ?: run {
                    _appDirInfo.postValue(Result.failure(IOException()))
                }
            } else {
                _appDirInfo.postValue(Result.failure(ExternalStorageNotAvailableException()))
            }
        }
    }
}