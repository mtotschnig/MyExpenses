package org.totschnig.myexpenses.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.ExchangeRateRepository
import org.totschnig.myexpenses.provider.INVALID_CALENDAR_ID
import org.totschnig.myexpenses.provider.PlannerUtils.Companion.checkLocalCalendar
import org.totschnig.myexpenses.provider.PlannerUtils.Companion.deleteLocalCalendar
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.filter.Operation
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.util.io.displayName
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SettingsViewModel(
    application: Application,
    val savedStateHandle: SavedStateHandle
) : ContentResolvingAndroidViewModel(application) {

    @Inject
    lateinit var exchangeRateRepository: ExchangeRateRepository

    data class AppDirInfo(
        val documentFile: DocumentFile,
        val displayName: String,
        val isWriteable: Boolean,
        val isDefault: Boolean
    )

    private val _appDirInfo: MutableLiveData<Result<AppDirInfo>> = MutableLiveData()
    val appDirInfo: LiveData<Result<AppDirInfo>> = _appDirInfo

    private val _appData: MutableLiveData<List<Pair<String, Long>>> = MutableLiveData()
    val appData: LiveData<List<Pair<String, Long>>> = _appData

    val hasStaleImages: Flow<Boolean>
        get() = contentResolver.observeQuery(
            TransactionProvider.STALE_IMAGES_URI,
            arrayOf("count(*)"), null, null, null, true
        ).mapToOne { cursor -> cursor.getInt(0) > 0 }

    fun logData() = liveData<Array<String>>(context = coroutineContext()) {
        getApplication<MyApplication>().getExternalFilesDir(null)?.let { dir ->
            File(dir, "logs").listFiles()
                ?.filter { it.length() > 0 }
                ?.sortedByDescending { it.lastModified() }
                ?.map { it.name }
                ?.let {
                    emit(it.toTypedArray())
                }
        }
    }

    fun loadAppData() {
        viewModelScope.launch(coroutineContext()) {
            AppDirHelper.getAppDir(getApplication()).onSuccess { dir ->
                _appData.postValue(
                    dir.listFiles()
                        .filter { (it.length() > 0) && !it.isDirectory }
                        .mapNotNull { file ->
                            file.name?.let {
                                Triple(it, file.length(), file.lastModified())
                            }
                        }
                        .sortedByDescending { it.third }
                        .map { it.first to it.second }
                )
            }
        }
    }

    private fun corruptedIdList(): LongArray? = contentResolver.call(
        TransactionProvider.DUAL_URI,
        TransactionProvider.METHOD_CHECK_CORRUPTED_DATA_987, null, null
    )?.getLongArray(TransactionProvider.KEY_RESULT)

    fun dataCorrupted() = liveData(context = coroutineContext()) {
        corruptedIdList()?.let {
            emit(it.size)
        }
    }

    fun shouldOfferCalendarRemoval() = liveData(context = coroutineContext()) {
        val localCalendar = if (ActivityCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
        ) contentResolver.checkLocalCalendar()
        else null
        emit(
            (localCalendar != null && localCalendar != INVALID_CALENDAR_ID) &&
                    (localCalendar != prefHandler.requireString(
                        PrefKey.PLANNER_CALENDAR_ID,
                        INVALID_CALENDAR_ID
                    )
                            || repository.count(
                        TransactionProvider.TEMPLATES_URI,
                        "${DatabaseConstants.KEY_PLANID} IS NOT NULL"
                    ) == 0)
        )
    }

    fun prettyPrintCorruptedData(currencyFormatter: ICurrencyFormatter) =
        liveData(context = coroutineContext()) {
            corruptedIdList()?.let { longs ->
                contentResolver.query(
                    TransactionProvider.EXTENDED_URI,
                    arrayOf(KEY_DATE, KEY_AMOUNT, KEY_CURRENCY, KEY_ACCOUNT_LABEL),
                    "$KEY_ROWID ${Operation.IN.getOp(longs.size)}",
                    longs.map { it.toString() }.toTypedArray(), null
                )?.use { cursor ->
                    cursor.asSequence.joinToString("\n") {
                        "(${it.getString(3)}): " +
                                Utils.convDateTime(
                                    it.getLong(0),
                                    Utils.ensureDateFormatWithShortYear(getApplication())
                                ) + " " + currencyFormatter.convAmount(
                            it.getLong(1),
                            currencyContext.get(it.getString(2))
                        )
                    }
                }?.let { emit(it) }
            }
        }

    fun loadAppDirInfo() {
        viewModelScope.launch(context = coroutineContext()) {
            _appDirInfo.postValue(
                AppDirHelper.getAppDirWithDefault(getApplication())
                    .map { (documentFile, isDefault) ->
                        AppDirInfo(
                            documentFile,
                            documentFile.displayName,
                            AppDirHelper.isWritableDirectory(documentFile),
                            isDefault
                        )
                    })
        }
    }

    fun storeSetting(key: String, value: String) = liveData(context = coroutineContext()) {
        emit(doAndWait(
            shouldWait = { it } //in case of error we show a new snackbar, so no need to delay
        ) {
            DbUtils.storeSetting(
                contentResolver,
                key,
                value
            ) != null
        })
    }

    private suspend inline fun <T> doAndWait(
        delayMillis: Long = 1000,
        shouldWait: (T) -> Boolean = { true },
        block: () -> T
    ): T {
        val start = System.nanoTime()
        val result = block()
        if (shouldWait(result)) {
            delay(delayMillis - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start))
        }
        return result
    }

    fun resetEquivalentAmounts() = liveData(context = coroutineContext()) {
        emit(
            contentResolver.call(
                TransactionProvider.DUAL_URI,
                TransactionProvider.METHOD_RESET_EQUIVALENT_AMOUNTS, null, null
            )
                ?.getInt(TransactionProvider.KEY_RESULT)
        )
    }

    fun clearExchangeRateCache() = liveData(context = coroutineContext()) {
        emit(exchangeRateRepository.deleteAll())
    }

    fun deleteAppFiles(files: Array<String>) = liveData(context = coroutineContext()) {
        with(appDirInfo.value?.getOrThrow()!!.documentFile) {
            emit(files.sumBy {
                findFile(it)?.delete()
                1
            })
        }
        loadAppData()
    }

    @RequiresPermission(allOf = [Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR])
    fun deleteLocalCalendar() = liveData(context = coroutineContext()) {
        emit(contentResolver.deleteLocalCalendar())
    }
}