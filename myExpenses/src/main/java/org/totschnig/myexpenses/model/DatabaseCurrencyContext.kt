package org.totschnig.myexpenses.model

import android.content.ContentResolver
import android.content.ContentValues
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.*
import org.totschnig.myexpenses.util.Utils
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class DatabaseCurrencyContext(
    private val contentResolver: ContentResolver,
    private val prefHandler: PrefHandler,
    private val application: MyApplication,
) : CurrencyContext {
    // Cache for performance, similar to PreferencesCurrencyContext
    private val instances = ConcurrentHashMap<String, CurrencyUnit>()

    override fun get(currencyCode: String): CurrencyUnit {
        if (currencyCode == DataBaseAccount.AGGREGATE_HOME_CURRENCY_CODE) return homeCurrencyUnit

        return instances.getOrPut(currencyCode) {
            fetchCurrency(currencyCode)
        }
    }

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
                val dbSymbol = cursor.getStringOrNull(KEY_SYMBOL)
                // Use getIntIfExists or getIntOrNull based on your Cursor extensions
                val dbFractionDigits = cursor.getIntIfExists(KEY_FRACTION_DIGITS)
                val label = cursor.getStringOrNull(KEY_LABEL) ?: currencyCode

                // If both are present, we have a fully defined asset in the DB
                if (dbSymbol != null && dbFractionDigits != null) {
                    return CurrencyUnit(currencyCode, dbSymbol, dbFractionDigits, label)
                }
            }
        }

        // 2. Fallback to Java Currency and legacy SharedPreferences
        val javaCurrency = try {
            Currency.getInstance(currencyCode)
        } catch (_: Exception) {
            null
        }

        // Support migration from legacy preference keys
        val prefSymbol =
            prefHandler.getString(currencyCode + "CustomCurrencySymbol", null)
        val prefFractionDigits =
            prefHandler.getInt(currencyCode + "CustomFractionDigits", -1)

        return if (javaCurrency != null) {
            CurrencyUnit(
                currencyCode,
                prefSymbol ?: javaCurrency.getSymbol(application.userPreferredLocale),
                if (prefFractionDigits != -1) prefFractionDigits else {
                    javaCurrency.defaultFractionDigits.takeIf { it != -1 }
                        ?: PreferencesCurrencyContext.DEFAULT_FRACTION_DIGITS
                },
                javaCurrency.displayName
            )
        } else {
            // Case for custom assets not yet in DB or Java (e.g. newly added crypto/stock)
            CurrencyUnit(
                currencyCode,
                prefSymbol ?: "¤",
                if (prefFractionDigits != -1) prefFractionDigits else PreferencesCurrencyContext.DEFAULT_FRACTION_DIGITS,
                currencyCode
            )
        }
    }

    override fun storeCustomFractionDigits(currencyCode: String, fractionDigits: Int) {
        val values = ContentValues().apply {
            put(KEY_FRACTION_DIGITS, fractionDigits)
        }
        update(currencyCode, values)
        instances.remove(currencyCode)
    }

    override fun storeCustomSymbol(currencyCode: String, symbol: String) {
        val values = ContentValues().apply {
            put(KEY_SYMBOL, symbol)
        }
        update(currencyCode, values)
        instances.remove(currencyCode)
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