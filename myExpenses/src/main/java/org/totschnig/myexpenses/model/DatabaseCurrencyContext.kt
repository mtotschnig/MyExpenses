package org.totschnig.myexpenses.model

import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.KEY_CODE
import org.totschnig.myexpenses.provider.KEY_COMMODITY_TYPE
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.provider.KEY_FRACTION_DIGITS
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.KEY_SYMBOL
import org.totschnig.myexpenses.provider.KEY_USAGES
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getEnum
import org.totschnig.myexpenses.provider.getIntOrNull
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.Utils
import java.util.Currency
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

const val DEFAULT_FRACTION_DIGITS = 8

open class DatabaseCurrencyContext(
    private val prefHandler: PrefHandler,
    private val application: MyApplication,
) : CurrencyContext {
    private val contentResolver: ContentResolver = application.contentResolver
    private val instances = ConcurrentHashMap<String, CurrencyUnit>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        scope.launch {
            preloadCurrencies()
        }
    }

    private fun Cursor.toCurrencyUnit(): CurrencyUnit {
        val databaseId = getLong(KEY_ROWID)
        val code = getString(KEY_CODE)
        val dbSymbol = getStringOrNull(KEY_SYMBOL)
        val dbFractionDigits = getIntOrNull(KEY_FRACTION_DIGITS)
        val dbLabel = getStringOrNull(KEY_LABEL)
        val commodityType = getEnum(KEY_COMMODITY_TYPE, CommodityType.FIAT)

        val (symbol, fractionDigits, label) = if (dbSymbol == null || dbFractionDigits == null || dbLabel == null) {
            val javaCurrency = try { Currency.getInstance(code) } catch (_: Exception) { null }
            Triple(
                dbSymbol ?: javaCurrency?.getSymbol(application.userPreferredLocale) ?: "¤",
                dbFractionDigits ?: javaCurrency?.defaultFractionDigits?.takeIf { it != -1 } ?: DEFAULT_FRACTION_DIGITS,
                dbLabel ?: javaCurrency?.getDisplayName(application.userPreferredLocale) ?:  CurrencyEnum.valueOf(code).description
            )
        } else Triple(dbSymbol, dbFractionDigits, dbLabel)

        return CurrencyUnit(
            code = code,
            symbol = symbol,
            fractionDigits = fractionDigits,
            description = label,
            commodityType = commodityType,
            databaseId = databaseId
        )
    }

    private fun preloadCurrencies() {
        val usedCurrencies = mutableSetOf<String>()
        // Ensure home currency is preloaded even if no accounts exist yet
        usedCurrencies.add(homeCurrencyString)

        contentResolver.query(
            TransactionProvider.ACCOUNTS_URI,
            arrayOf(KEY_CURRENCY),
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                cursor.getStringOrNull(KEY_CURRENCY)?.let { usedCurrencies.add(it) }
            }
        }

        if (usedCurrencies.isEmpty()) return

        val placeholders = usedCurrencies.joinToString(",") { "?" }
        val selection = "$KEY_CODE IN ($placeholders)"
        val selectionArgs = usedCurrencies.toTypedArray()

        contentResolver.query(
            TransactionProvider.CURRENCIES_URI,
            null,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                cursor.toCurrencyUnit().let { unit ->
                    instances[unit.code] = unit
                }
            }
        }
    }

    override val homeCurrencyUnit: CurrencyUnit
        get() = get(homeCurrencyString)

    override fun get(currencyCode: String): CurrencyUnit {
        if (currencyCode == DataBaseAccount.AGGREGATE_HOME_CURRENCY_CODE) return homeCurrencyUnit

        return instances.getOrPut(currencyCode) {
            fetchCurrency(currencyCode)
        }
    }

    override fun getAll(): Flow<List<CurrencyUnit>> = contentResolver.observeQuery(
        TransactionProvider.CURRENCIES_URI,
        null,
        null,
        null,
        "$KEY_USAGES DESC, $KEY_CODE",
        true
    ).mapToList(mapper = { cursor ->
        cursor.toCurrencyUnit()
    })

    private fun fetchCurrency(currencyCode: String): CurrencyUnit {
        // 1. Attempt to load customized metadata from the database
        contentResolver.query(
            TransactionProvider.CURRENCIES_URI,
            null,
            "$KEY_CODE = ?",
            arrayOf(currencyCode),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.toCurrencyUnit()

            }
        }
        throw IllegalArgumentException("$currencyCode not defined")
    }


    override fun storeCustomFractionDigits(currencyCode: String, fractionDigits: Int?) {
        instances.remove(currencyCode)
        val values = ContentValues().apply {
            put(KEY_FRACTION_DIGITS, fractionDigits)
        }
        update(currencyCode, values)
    }

    override fun storeCustomSymbol(currencyCode: String, symbol: String) {
        instances.remove(currencyCode)
        val values = ContentValues().apply {
            put(KEY_SYMBOL, symbol)
        }
        update(currencyCode, values)
    }

    private fun update(currencyCode: String, values: ContentValues) {
        val updated = contentResolver.update(
            TransactionProvider.CURRENCIES_URI,
            values,
            "$KEY_CODE = ?",
            arrayOf(currencyCode)
        )
        require(updated == 1)
    }

    override fun ensureFractionDigitsAreCached(currency: CurrencyUnit) {
        // Persist to DB to ensure consistency across the app
        storeCustomFractionDigits(currency.code, currency.fractionDigits)
    }

    override fun invalidateHomeCurrency() {
        instances.remove(homeCurrencyString)
        instances.remove(DataBaseAccount.AGGREGATE_HOME_CURRENCY_CODE)
    }

    override val homeCurrencyString: String
        get() = prefHandler.getString(PrefKey.HOME_CURRENCY, null)
            ?: localCurrency.currencyCode

    override val localCurrency: Currency by lazy {
        Utils.getCountryFromTelephonyManager(application)?.let {
            try {
                Currency.getInstance(Locale("", it))
            } catch (_: Exception) {
                null
            }
        } ?: Utils.getSaveDefault()
    }
}