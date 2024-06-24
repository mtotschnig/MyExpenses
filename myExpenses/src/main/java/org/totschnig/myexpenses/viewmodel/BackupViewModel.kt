package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.doBackup
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.crypt.EncryptionHelper
import org.totschnig.myexpenses.util.io.displayName
import java.io.IOException

class BackupViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    sealed class BackupState {
        class Prepared(val appDir: Result<DocumentFile>) : BackupState()
        object Running : BackupState()
        class Completed(val result: Result<Triple<DocumentFile, String, Either<List<DocumentFile>, List<Boolean>>>>) :
            BackupState()

        class Purged(val result: Result<Int>) : BackupState()
    }

    private val backupState = MutableLiveData<BackupState>()

    fun getBackupState(): LiveData<BackupState> {
        return backupState
    }

    fun doBackup(withSync: Boolean, lenientMode: Boolean) {
        viewModelScope.launch(coroutineDispatcher) {
            backupState.postValue(BackupState.Running)
            backupState.postValue(
                BackupState.Completed(
                    doBackup(
                        getApplication(),
                        prefHandler,
                        withSync,
                        lenientMode
                    ).mapCatching { (backupFile, oldBackups) ->
                        val requireConfirmation =
                            prefHandler.getBoolean(PrefKey.PURGE_BACKUP_REQUIRE_CONFIRMATION, true)
                        val extraData = if (requireConfirmation || oldBackups.isEmpty())
                            Either.Left(oldBackups) else
                            Either.Right(oldBackups.map { it.delete() })
                        Triple(
                            backupFile,
                            backupFile.displayName,
                            extraData
                        )
                    })
            )
        }
    }

    fun isEncrypted(uri: Uri) = liveData(context = coroutineContext()) {
        emit(
            runCatching {
                getApplication<MyApplication>().contentResolver.openInputStream(uri).use {
                    if (it == null) throw IOException("Unable to open file $uri")
                    EncryptionHelper.isEncrypted(it)
                }
            })
    }

    fun prepare() {
        viewModelScope.launch(coroutineDispatcher) {
            backupState.postValue(BackupState.Prepared(AppDirHelper.checkAppDir(getApplication())))
        }
    }

    fun purgeBackups() {
        viewModelScope.launch(coroutineDispatcher) {
            (backupState.value as? BackupState.Completed)?.let {
                backupState.postValue(
                    BackupState.Purged(
                        runCatching {
                            @Suppress("DEPRECATION")
                            it.result.getOrThrow().third.fold(
                                ifLeft = { list ->
                                    list.sumBy {
                                        if (it.delete()) 1 else 0
                                    }
                                },
                                ifRight = {
                                    throw IllegalStateException()
                                }
                            )
                        }
                    ))
            }
        }
    }

    companion object {
        fun purgeResult2Message(context: Context, result: List<Boolean>): String {
            val deleted = result.count { it }
            val failed = result.count { !it }
            return buildList {
                if (deleted > 0) {
                    add(
                        context.resources.getQuantityString(
                            R.plurals.purge_backup_success,
                            deleted,
                            deleted
                        )
                    )
                }
                if (failed > 0) {
                    add(
                        context.resources.getQuantityString(
                            R.plurals.purge_backup_failure,
                            failed,
                            failed
                        )
                    )
                }
            }.joinToString(" ")
        }
    }
}