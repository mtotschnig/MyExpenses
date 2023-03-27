@file:Suppress("DEPRECATION")

package org.totschnig.myexpenses.testutils

import android.content.ContentResolver
import android.content.Context
import android.content.pm.ProviderInfo
import android.content.res.Configuration
import android.test.IsolatedContext
import android.test.ProviderTestCase2
import android.test.RenamingDelegatingContext
import android.test.mock.MockContentResolver
import android.test.mock.MockContext
import androidx.test.platform.app.InstrumentationRegistry
import org.totschnig.myexpenses.TestApp
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.TransactionProvider

open class BaseProviderTest : ProviderTestCase2<TransactionProvider>(TransactionProvider::class.java, TransactionProvider.AUTHORITY) {
    lateinit var transactionProvider: TransactionProvider
    lateinit var targetContextWrapper: Context
    lateinit var resolver: MockContentResolver

    val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    val app: TestApp
        get() = targetContext.applicationContext as TestApp

    val homeCurrency: CurrencyUnit
        get() = app.appComponent.homeCurrencyProvider().homeCurrencyUnit

    fun buildAccount(label: String, openingBalance: Long = 0L) =
        Account(label, homeCurrency, openingBalance, AccountType.CASH).also { it.save(homeCurrency) }

    fun getTransactionFromDb(id: Long): Transaction? = Transaction.getInstanceFromDb(id, homeCurrency)

    @Throws(Exception::class)
    override fun setUp() {
        transactionProvider = TransactionProvider::class.java.newInstance()
        resolver = MockContentResolver()
        val filenamePrefix = "test."
        targetContextWrapper = RenamingDelegatingContext(
                DelegatedMockContext(resolver),  // The context that most methods are
                //delegated to
                context,  // The context that file methods are delegated to
                filenamePrefix)
        assertNotNull(transactionProvider)
        val providerInfo = ProviderInfo()
        providerInfo.authority = TransactionProvider.AUTHORITY
        transactionProvider.attachInfo(IsolatedContext(resolver, context), providerInfo)
        resolver.addProvider(TransactionProvider.AUTHORITY, transactionProvider)
    }

    @Throws(Exception::class)
    override fun tearDown() {}

    override fun getMockContentResolver() = resolver

    override fun getProvider() = transactionProvider

    private inner class DelegatedMockContext(val resolver: ContentResolver) : MockContext() {
        override fun createConfigurationContext(overrideConfiguration: Configuration) = this
        override fun getResources() = context.getResources()
        override fun getDir(name: String, mode: Int) = context.getDir("mockcontext2_$name", mode)
        override fun getApplicationContext() = this
        override fun getContentResolver() = resolver
    }
}