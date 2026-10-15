package org.totschnig.myexpenses.provider

import org.junit.Before
import org.robolectric.Robolectric
import org.totschnig.myexpenses.BaseTestWithRepository

abstract class TransactionProviderBaseTest: BaseTestWithRepository() {
    protected lateinit var provider: BaseTransactionProvider
    protected var accountId: Long = 0

    @Before
    fun setUpBase() {
        provider = Robolectric.buildContentProvider(TransactionProvider::class.java).create().get()
        accountId = insertAccount("Test Account")
    }
}