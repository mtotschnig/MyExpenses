package org.totschnig.myexpenses.viewmodel

import android.app.Application
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CODE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.Currency
import java.text.Collator

open class CurrencyViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {

    val currencies: Flow<List<Currency>>
        get() {
            val collator: Collator? = try {
                Collator.getInstance()
            } catch (e: Exception) {
                CrashHandler.report(e)
                null
            }
            return contentResolver.observeQuery(
                TransactionProvider.CURRENCIES_URI, null, null, null,
                if (collator == null) KEY_CODE else null, true
            )
                .mapToList { Currency.create(it, userLocaleProvider.getUserPreferredLocale()) }
                .map { list ->
                    if (collator != null) list.sortedWith { lhs, rhs ->
                        rhs.usages.compareTo(lhs.usages).takeIf { it != 0 }
                            ?: lhs.sortClass.compareTo(rhs.sortClass).takeIf { it != 0 }
                            ?: collator.compare(lhs.toString(), rhs.toString())
                    } else list
                }
        }

    val default: Currency
        get() = Currency.create(
            Utils.getHomeCurrency().code,
            userLocaleProvider.getUserPreferredLocale()
        )
}
