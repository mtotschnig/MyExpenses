package org.totschnig.myexpenses.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.savePrice
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefHandler.Companion.AUTOMATIC_EXCHANGE_RATE_DOWNLOAD_PREF_KEY_PREFIX
import org.totschnig.myexpenses.preference.PrefHandler.Companion.SERVICE_DEACTIVATED
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.TimePreference
import org.totschnig.myexpenses.preference.dynamicExchangeRates
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMODITY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PRICES
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.retrofit.ExchangeRateApi
import org.totschnig.myexpenses.retrofit.ExchangeRateService
import org.totschnig.myexpenses.util.ContribUtils
import org.totschnig.myexpenses.util.NotificationBuilderWrapper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.licence.LicenceHandler
import timber.log.Timber
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DailyExchangeRateDownloadService(context: Context, workerParameters: WorkerParameters) :
    NotifyingBaseWorker(context, workerParameters) {

    @Inject
    lateinit var exchangeRateService: ExchangeRateService

    @Inject
    lateinit var currencyContext: CurrencyContext

    @Inject
    lateinit var repository: Repository

    @Inject
    lateinit var licenceHandler: LicenceHandler

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
                setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
                setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 40, TimeUnit.MINUTES)
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

        if (!licenceHandler.hasTrialAccessTo(ContribFeature.AUTOMATIC_FX_DOWNLOAD)) {
            prefHandler.putBoolean(PrefKey.AUTOMATIC_EXCHANGE_RATE_DOWNLOAD, false)
            ContribUtils.showContribNotification(applicationContext, ContribFeature.AUTOMATIC_FX_DOWNLOAD)
            return Result.failure()
        }

        val result: List<kotlin.Result<Unit>>? = applicationContext.contentResolver.query(
            TransactionProvider.ACCOUNTS_MINIMAL_URI,
            arrayOf("distinct $KEY_CURRENCY"),
            "${datastore.dynamicExchangeRates.first()} AND $KEY_CURRENCY != ? AND NOT EXISTS(SELECT 1 from $TABLE_PRICES where $KEY_COMMODITY = $TABLE_ACCOUNTS.$KEY_CURRENCY and $KEY_DATE = date('now'))",
            arrayOf(currencyContext.homeCurrencyString),
            null
        )
            ?.useAndMapToList {
                val currency = it.getString(0)
                prefHandler.getString("${AUTOMATIC_EXCHANGE_RATE_DOWNLOAD_PREF_KEY_PREFIX}${currency}")
                    ?.takeIf { it != SERVICE_DEACTIVATED }
                    ?.let { ExchangeRateApi.getByName(it) to currency }
            }
            ?.filterNotNull()
            ?.groupBy({ it.first }, { it.second })
            ?.map { (source, symbols) ->
                runCatching {
                    Timber.d("Loading ${symbols.joinToString()} from $source (attempt $runAttemptCount)")
                    val apiKey =
                        (source as? ExchangeRateApi.SourceWithApiKey)?.requireApiKey(prefHandler)
                    val base = currencyContext.homeCurrencyString
                    val (date,rates) = exchangeRateService.getLatest(
                        source,
                        apiKey,
                        base,
                        symbols
                    )
                    symbols.forEachIndexed { index, currency ->
                        repository.savePrice(
                            currencyContext.homeCurrencyUnit,
                            currencyContext[currency],
                            date,
                            source,
                            BigDecimal.valueOf(rates[index])
                        )
                    }
                }.onFailure {
                    CrashHandler.report(it)
                }
            }
        return if (result?.any { it.isFailure } == true) {
            if (runAttemptCount == 6) {
                notify("Download of exchange rates failed five times. Giving up.")
                Result.failure()
            } else Result.retry()
        } else {
            enqueueOrCancel(applicationContext, prefHandler)
            Result.success()
        }
    }
}