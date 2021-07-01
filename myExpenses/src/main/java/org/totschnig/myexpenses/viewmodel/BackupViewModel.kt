package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.doBackup
import org.totschnig.myexpenses.util.io.FileUtils
import javax.inject.Inject

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    @Inject
    lateinit var prefHandler: PrefHandler
    @Inject
    lateinit var coroutineDispatcher: CoroutineDispatcher

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
}