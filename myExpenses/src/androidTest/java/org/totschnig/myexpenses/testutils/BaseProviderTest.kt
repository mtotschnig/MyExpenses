@file:Suppress("DEPRECATION")

package org.totschnig.myexpenses.testutils

import android.content.pm.ProviderInfo
import android.content.res.Configuration
import android.test.IsolatedContext
import android.test.ProviderTestCase2
import android.test.RenamingDelegatingContext
import android.test.mock.MockContentResolver
import android.test.mock.MockContext
import org.totschnig.myexpenses.provider.TransactionProvider

open class BaseProviderTest : ProviderTestCase2<TransactionProvider>(TransactionProvider::class.java, TransactionProvider.AUTHORITY) {
    lateinit var transactionProvider: TransactionProvider
    lateinit var resolver: MockContentResolver

    @Throws(Exception::class)
    override fun setUp() {
        transactionProvider = TransactionProvider::class.java.newInstance()
        resolver = MockContentResolver()
        val filenamePrefix = "test."
        val targetContextWrapper = RenamingDelegatingContext(
                DelegatedMockContext(),  // The context that most methods are
                //delegated to
                context,  // The context that file methods are delegated to
                filenamePrefix)
        assertNotNull(transactionProvider)
        val providerInfo = ProviderInfo()
        providerInfo.authority = TransactionProvider.AUTHORITY
        transactionProvider.attachInfo(IsolatedContext(resolver, targetContextWrapper), providerInfo)
        resolver.addProvider(TransactionProvider.AUTHORITY, transactionProvider)
    }

    @Throws(Exception::class)
    override fun tearDown() {}

    override fun getMockContentResolver() = resolver

    override fun getProvider() = transactionProvider

    private inner class DelegatedMockContext : MockContext() {
        override fun createConfigurationContext(overrideConfiguration: Configuration) = this
        override fun getResources() = context.getResources()
        override fun getDir(name: String, mode: Int) = context.getDir("mockcontext2_$name", mode)
        override fun getApplicationContext() = this
    }
}