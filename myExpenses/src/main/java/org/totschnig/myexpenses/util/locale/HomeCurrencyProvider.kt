package org.totschnig.myexpenses.util.locale

import android.content.Context
import org.totschnig.myexpenses.model.CurrencyUnit
import java.util.*

interface HomeCurrencyProvider {

    val homeCurrencyString: String
    val homeCurrencyUnit: CurrencyUnit
    val localCurrency: Currency
}