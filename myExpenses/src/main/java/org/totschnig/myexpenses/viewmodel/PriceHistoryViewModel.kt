package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.net.Uri
import androidx.core.os.BundleCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.deletePrice
import org.totschnig.myexpenses.db2.savePrice
import org.totschnig.myexpenses.export.createFileFailure
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMODITY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAX_VALUE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SOURCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getLocalDate
import org.totschnig.myexpenses.provider.mapToListWithExtra
import org.totschnig.myexpenses.retrofit.ExchangeRateApi
import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.ExchangeRateHandler
import org.totschnig.myexpenses.util.calculateRealExchangeRate
import org.totschnig.myexpenses.util.io.displayName
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.data.Price
import timber.log.Timber
import java.io.IOException
import java.io.OutputStreamWriter
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject

class PriceHistoryViewModel(application: Application, val savedStateHandle: SavedStateHandle) :
    ContentResolvingAndroidViewModel(application) {

    private val _batchDownloadResult: MutableStateFlow<String?> = MutableStateFlow(null)
    val batchDownloadResult: Flow<String> = _batchDownloadResult.asStateFlow().filterNotNull()
    private val _exportResult: MutableStateFlow<Result<Pair<Uri, String>>?> = MutableStateFlow(null)
    val exportResult: Flow<Result<Pair<Uri, String>>> = _exportResult.asStateFlow().filterNotNull()
    private val _importResult: MutableStateFlow<Result<String>?> = MutableStateFlow(null)
    val importResult: Flow<Result<String>> = _importResult.asStateFlow().filterNotNull()

    fun messageShown() {
        _batchDownloadResult.update { null }
        _exportResult.update { null }
        _importResult.update { null }
    }

    @Inject
    lateinit var exchangeRateHandler: ExchangeRateHandler

    val commodity: String
        get() = savedStateHandle.get<String>(KEY_COMMODITY)!!

    private val inverseRatePreferenceKey = booleanPreferencesKey("inverseRate_$commodity")

    val inverseRate: Flow<Boolean> by lazy {
        dataStore.data.map { preferences ->
            preferences[inverseRatePreferenceKey] == true
        }
    }

    suspend fun persistInverseRate(inverseRate: Boolean) {
        dataStore.edit { preference ->
            preference[inverseRatePreferenceKey] = inverseRate
        }
    }

    val homeCurrency
        get() = currencyContext.homeCurrencyString

    val relevantSources: List<ExchangeRateApi> by lazy {
        exchangeRateHandler.relevantSources(commodity).also {
            if (it.size > 1) {
                userSelectedSource = it[0]
            }
        }
    }

    var userSelectedSource: ExchangeRateApi? = null

    val effectiveSource: ExchangeRateApi?
        get() = when (relevantSources.size) {
            0 -> null
            1 -> relevantSources.first()
            else -> userSelectedSource
        }

    val pricesWithMissingDates by lazy {
        contentResolver.observeQuery(
            uri = TransactionProvider.PRICES_URI
                .buildUpon()
                .appendQueryParameter(KEY_COMMODITY, commodity)
                .build(),
            projection = arrayOf(KEY_DATE, KEY_SOURCE, KEY_VALUE),
            notifyForDescendants = true
        ).mapToListWithExtra {
            Price(
                date = it.getLocalDate(0),
                source = ExchangeRateSource.getByName(it.getString(1)),
                value = calculateRealExchangeRate(
                    it.getDouble(2),
                    currencyContext[commodity],
                    currencyContext.homeCurrencyUnit
                )
            )
        }
            .map {
                it.second.fillInMissingDates(
                    end = BundleCompat.getSerializable(
                        it.first,
                        KEY_MAX_VALUE,
                        LocalDate::class.java
                    )
                )
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())
    }


    fun List<Price>.fillInMissingDates(
        start: LocalDate? = null,
        end: LocalDate? = null,
    ): Map<LocalDate, Price?> {
        val seed = start ?: LocalDate.now()

        val maxDate = listOfNotNull(end, minOfOrNull { it.date }).minOrNull() ?: seed
        val allDates = generateSequence(seed) { it.minusDays(1) }
            .takeWhile { it >= maxDate }
            .toList()

        return allDates.associateWithTo(LinkedHashMap()) { date ->
            find { it.date == date }
        }
    }

    fun deletePrice(price: Price): LiveData<Boolean> =
        liveData(context = coroutineContext()) {
            emit(
                repository.deletePrice(price.date, price.source, homeCurrency, commodity) == 1
            )
        }


    fun savePrice(date: LocalDate, value: BigDecimal): LiveData<Int> =
        liveData(context = coroutineContext()) {
            emit(
                repository.savePrice(
                    currencyContext.homeCurrencyUnit,
                    currencyContext[commodity],
                    date,
                    ExchangeRateSource.User,
                    value
                )
            )
        }

    suspend fun loadFromNetwork(
        source: ExchangeRateApi,
        date: LocalDate,
    ) = exchangeRateHandler.loadFromNetwork(
        source,
        date,
        commodity,
        currencyContext.homeCurrencyString
    )

    suspend fun loadTimeSeries(
        source: ExchangeRateApi,
        start: LocalDate,
        end: LocalDate,
    ) {
        val except  = if (source.hasTimeSeriesRequest) emptySet() else
            pricesWithMissingDates.value.filter { it.key in start..end  && it.value != null }.keys
        Timber.d("loadTimeSeries skip : ${except.joinToString()}")
        val (count, exception) = exchangeRateHandler.loadTimeSeries(
            source,
            start,
            end,
            except,
            commodity,
            currencyContext.homeCurrencyString
        )
        _batchDownloadResult.update {
            getString(R.string.batch_download_result, count) + (exception?.let {
                " " + it.safeMessage
            } ?: "")
        }
    }

    fun export() {
        viewModelScope.launch(context = coroutineContext()) {
            val context = getApplication<MyApplication>()
            val fileName = "$commodity-${currencyContext.homeCurrencyString}"
            val currentPricesMap = pricesWithMissingDates.value
            _exportResult.update {
                AppDirHelper.getAppDir(context).mapCatching { destDir ->
                    AppDirHelper.timeStampedFile(
                        destDir,
                        fileName,
                        ExportFormat.CSV.mimeType, "csv"
                    ) ?: throw createFileFailure(context, destDir, fileName)
                }.mapCatching {
                    context.contentResolver.openOutputStream(it.uri, "w").use {
                        OutputStreamWriter(it, StandardCharsets.UTF_8).use { out ->
                            currentPricesMap.entries
                                .filter { it.value != null }
                                .sortedBy { it.key }
                                .forEach { (date, price) ->
                                    price?.let {
                                        out.appendLine("$date,${it.value.toPlainString()}")
                                    }
                                }
                        }
                    }
                    it.uri to it.displayName
                }
            }
        }
    }

    fun importPricesFromUri(fileUri: Uri) {
        viewModelScope.launch {
            val context = getApplication<MyApplication>()
            var importedCount = 0
            var failedLines = 0

            _importResult.update {
                runCatching {
                    context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                        // Use BufferedReader for efficient line-by-line reading
                        inputStream.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
                            lines.forEachIndexed { index, line ->
                                try {
                                    val parts = line.split(',')
                                    if (parts.size < 2) { // Expecting at least Date, Source, Value
                                        Timber.w("Skipping malformed CSV line ${index + 2}: $line - Not enough parts")
                                        failedLines++
                                        return@forEachIndexed
                                    }

                                    val dateString = parts[0].trim()
                                    val valueString = parts[1].trim()

                                    if (dateString.isBlank() || valueString.isBlank()) {
                                        Timber.w("Skipping malformed CSV line ${index + 2}: $line - Empty parts")
                                        failedLines++
                                        return@forEachIndexed
                                    }

                                    val date = LocalDate.parse(dateString)
                                    val value = BigDecimal(valueString)

                                    repository.savePrice(
                                        currencyContext.homeCurrencyUnit,
                                        currencyContext[commodity],
                                        date,
                                        ExchangeRateSource.Import,
                                        value
                                    )
                                    importedCount++

                                } catch (e: DateTimeParseException) {
                                    Timber.e(
                                        e,
                                        "Error parsing date in CSV line ${index + 1}: $line"
                                    )
                                    failedLines++
                                } catch (e: NumberFormatException) {
                                    Timber.e(
                                        e,
                                        "Error parsing value in CSV line ${index + 1}: $line"
                                    )
                                    failedLines++
                                } catch (e: Exception) {
                                    Timber.e(
                                        e,
                                        "Generic error processing CSV line ${index + 1}: $line"
                                    )
                                    failedLines++
                                }
                            }
                        }
                    } ?: throw IOException("Failed to open input stream for URI: $fileUri")

                    if (importedCount > 0 || failedLines > 0) {
                        buildString {
                            if (importedCount > 0) {
                                appendLine(context.resources.getQuantityString(R.plurals.import_prices_success_message, importedCount, importedCount))
                            }
                            if (failedLines > 0) {
                                appendLine(context.resources.getQuantityString(R.plurals.import_prices_failure_message, failedLines, failedLines))
                            }
                        }
                    } else {
                        context.getString(R.string.no_data)
                    }
                }
            }
        }
    }
}