package org.totschnig.myexpenses

import androidx.test.core.app.ApplicationProvider
import org.mockito.Mockito
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.CurrencyFormatter

open class BaseTestWithRepository {
    val currencyContext: CurrencyContext = Mockito.mock(CurrencyContext::class.java)
    val repository: Repository = Repository(
        ApplicationProvider.getApplicationContext<MyApplication>(),
        currencyContext,
        Mockito.mock(CurrencyFormatter::class.java),
        Mockito.mock(PrefHandler::class.java)
    )
}