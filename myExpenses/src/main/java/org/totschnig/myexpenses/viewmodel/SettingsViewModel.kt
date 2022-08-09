package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.exception.ExternalStorageNotAvailableException
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.ExchangeRateRepository
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.filter.WhereFilter.Operation
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.util.io.FileUtils
import java.io.File
import java.io.IOException
import javax.inject.Inject

class SettingsViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    @Inject
    lateinit var exchangeRateRepository: ExchangeRateRepository

    private val _appDirInfo: MutableLiveData<Result<Pair<String, Boolean>>> = MutableLiveData()
    val appDirInfo: LiveData<Result<Pair<String, Boolean>>> = _appDirInfo
    val hasStaleImages: LiveData<Boolean> by lazy {
        val liveData = MutableLiveData<Boolean>()
        disposable = briteContentResolver.createQuery(
            TransactionProvider.STALE_IMAGES_URI,
            arrayOf("count(*)"), null, null, null, true
        )
            .mapToOne { cursor -> cursor.getInt(0) > 0 }
            .subscribe { liveData.postValue(it) }
        return@lazy liveData
    }

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

    private fun corruptedIdList(): LongArray? = contentResolver.call(
        TransactionProvider.DUAL_URI,
        TransactionProvider.METHOD_CHECK_CORRUPTED_DATA_987, null, null
    )?.getLongArray(TransactionProvider.KEY_RESULT)

    fun dataCorrupted() = liveData(context = coroutineContext()) {
        corruptedIdList()?.let {
            emit(it.size)
        }
    }

    fun prettyPrintCorruptedData(currencyFormatter: CurrencyFormatter) =
        liveData(context = coroutineContext()) {
            corruptedIdList()?.let { longs ->
                contentResolver.query(
                    Transaction.EXTENDED_URI,
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
            if (AppDirHelper.isExternalStorageAvailable) {
                AppDirHelper.getAppDir(getApplication())?.let {
                    _appDirInfo.postValue(
                        Result.success(
                            Pair(
                                FileUtils.getPath(
                                    getApplication(),
                                    it.uri
                                ), AppDirHelper.isWritableDirectory(it)
                            )
                        )
                    )
                } ?: run {
                    _appDirInfo.postValue(Result.failure(IOException()))
                }
            } else {
                _appDirInfo.postValue(Result.failure(ExternalStorageNotAvailableException()))
            }
        }
    }

    fun storeSetting(key: String, value: String) = liveData(context = coroutineContext()) {
        emit(DbUtils.storeSetting(contentResolver, key, value) != null)
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
}