package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.doBackup
import org.totschnig.myexpenses.util.crypt.EncryptionHelper
import org.totschnig.myexpenses.util.io.FileUtils
import java.io.IOException

class BackupViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    sealed class BackupState {
        object Running : BackupState()
        class Error(val throwable: Throwable) : BackupState()
        class Success(val result: Pair<DocumentFile, String>) : BackupState()
    }

    private val backupState = MutableLiveData<BackupState>()

    fun getBackupState(): LiveData<BackupState> {
        return backupState
    }

    fun doBackup(password: String?, withSync: Boolean) {
        viewModelScope.launch(coroutineDispatcher) {
            backupState.postValue(BackupState.Running)
            doBackup(
                getApplication(),
                password,
                if (withSync) prefHandler.getString(PrefKey.AUTO_BACKUP_CLOUD, null) else null
            ).onSuccess {
                backupState.postValue(
                    BackupState.Success(
                        it to FileUtils.getPath(
                            getApplication(),
                            it.uri
                        )
                    )
                )
            }.onFailure {
                backupState.postValue(BackupState.Error(it))
            }
        }
    }

    fun isEncrypted(uri: Uri) = liveData(context = coroutineContext()) {
        emit(
            runCatching {
                getApplication<MyApplication>().contentResolver.openInputStream(uri).use {
                    if (it == null) throw(IOException("Unable to open file $uri"))
                    EncryptionHelper.isEncrypted(it)
            }
        })
    }
}