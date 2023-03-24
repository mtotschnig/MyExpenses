package org.totschnig.myexpenses.util.locale

import android.content.Context
import java.util.*

interface HomeCurrencyProvider {
    fun getLocalCurrency(context: Context): Currency
}