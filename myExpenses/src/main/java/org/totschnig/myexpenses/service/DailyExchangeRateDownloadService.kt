package org.totschnig.myexpenses.service

import android.content.ContentValues
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.SelectCategoryMoveTargetDialogFragment.Companion.KEY_SOURCE
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefHandler.Companion.AUTOMATIC_EXCHANGE_RATE_DOWNLOAD_PREF_KEY_PREFIX
import org.totschnig.myexpenses.preference.PrefHandler.Companion.SERVICE_DEACTIVATED
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.TimePreference
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMODITY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DYNAMIC
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.retrofit.ExchangeRateService
import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import org.totschnig.myexpenses.util.NotificationBuilderWrapper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.safeMessage
import timber.log.Timber
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DailyExchangeRateDownloadService(context: Context, workerParameters: WorkerParameters) :
    NotifyingBaseWorker(context, workerParameters) {

    @Inject
    lateinit var exchangeRateService: ExchangeRateService

    @Inject
    lateinit var currencyContext: CurrencyContext

    init {
        context.injector.inject(this)
    }

    companion object {
        private const val WORK_NAME = "DailyExchangeRateService"

        private fun buildWorkRequest(initialDelayMillis: Long?): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<DailyExchangeRateDownloadService>().apply {
                initialDelayMillis?.let {
                    setInitialDelay(it, TimeUnit.MILLISECONDS)
                }
            }.build()
        }

        private fun WorkManager.cancelWork() = cancelUniqueWork(WORK_NAME)

        private fun WorkManager.enqueue(initialDelayMillis: Long?) {
            enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                buildWorkRequest(initialDelayMillis)
            )
        }

        fun enqueueOrCancel(context: Context, prefHandler: PrefHandler) {
            val workManager = WorkManager.getInstance(context)
            if (prefHandler.getBoolean(PrefKey.AUTOMATIC_EXCHANGE_RATE_DOWNLOAD, false)) {
                workManager.enqueue(
                    TimePreference.getScheduledTime(12, 0)
                )
            } else {
                workManager.cancelWork()
            }
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelWork()
        }

        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueue(null)
        }
    }

    override val channelId: String = NotificationBuilderWrapper.CHANNEL_ID_DEFAULT
    override val notificationId =
        NotificationBuilderWrapper.NOTIFICATION_EXCHANGE_RATE_DOWNLOAD_ERROR
    override val notificationTitleResId = R.string.pref_category_exchange_rates

    override suspend fun doWork(): Result {
        applicationContext.contentResolver.query(
            TransactionProvider.ACCOUNTS_URI,
            arrayOf("distinct $KEY_CURRENCY"),
            "$KEY_DYNAMIC AND $KEY_CURRENCY != ?",
            arrayOf(currencyContext.homeCurrencyString),
            null
        )
            ?.useAndMapToList {
                val currency = it.getString(0)
                prefHandler.getString("${AUTOMATIC_EXCHANGE_RATE_DOWNLOAD_PREF_KEY_PREFIX}${currency}")
                    ?.takeIf { it != SERVICE_DEACTIVATED }
                    ?.let { ExchangeRateSource.getById(it) to currency }
            }
            ?.filterNotNull()
            ?.groupBy({ it.first }, { it.second })
            ?.forEach { (source, symbols) ->
                runCatching {
                    Timber.d("Loading ${symbols.joinToString()} from $source")
                    val apiKey =
                        (source as? ExchangeRateSource.SourceWithApiKey)?.requireApiKey(prefHandler)
                    val base = currencyContext.homeCurrencyString
                    val rates = exchangeRateService.getLatest(
                        source,
                        apiKey,
                        base,
                        symbols
                    )
                    symbols.forEachIndexed { index, currency ->
                        val (date, rate) = rates[index]
                        storeInDb(base, currency, date, rate, source)
                    }
                }.onFailure {
                    CrashHandler.report(it)
                    notify(it.safeMessage)
                }
            }
        enqueueOrCancel(applicationContext, prefHandler)
        return Result.success()
    }

    private fun storeInDb(
        base: String,
        other: String,
        date: LocalDate,
        rate: Double,
        source: ExchangeRateSource,
    ) {
        applicationContext.contentResolver.insert(
            TransactionProvider.PRICES_URI,
            ContentValues().apply {
                put(KEY_CURRENCY, base)
                put(KEY_COMMODITY, other)
                put(KEY_DATE, date.toString())
                put(KEY_SOURCE, source.id)
                put(KEY_VALUE, rate)
            })
    }
}