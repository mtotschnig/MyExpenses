package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CODE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import org.totschnig.myexpenses.viewmodel.data.Currency
import java.text.Collator
import java.util.*
import javax.inject.Inject

open class CurrencyViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

     @Inject
     lateinit var userLocaleProvider: UserLocaleProvider

    init {
        (application as MyApplication).appComponent.inject(this)
    }

    private val currencies by lazy {
        val liveData = MutableLiveData<List<Currency>>()
        val collator: Collator? = try {
            Collator.getInstance()
        } catch (e: Exception) {
            CrashHandler.report(e)
            null
        }
        disposable = briteContentResolver.createQuery(TransactionProvider.CURRENCIES_URI, null, null, null,
                if (collator == null) KEY_CODE else null, true)
                .mapToList { Currency.create(it, userLocaleProvider.getUserPreferredLocale()) }
                .subscribe { currencies ->
                    if (collator != null) {
                        currencies.sortWith { lhs, rhs ->
                            Utils.compare(lhs.sortClass(), rhs.sortClass()).takeIf { it != 0 }
                                    ?: collator.compare(lhs.toString(), rhs.toString())
                        }
                    }
                    liveData.postValue(currencies)
                }
        return@lazy liveData
    }

    val default: Currency
        get() = Currency.create(Utils.getHomeCurrency().code, userLocaleProvider.getUserPreferredLocale())


    fun getCurrencies(): LiveData<List<Currency>> = currencies
}
