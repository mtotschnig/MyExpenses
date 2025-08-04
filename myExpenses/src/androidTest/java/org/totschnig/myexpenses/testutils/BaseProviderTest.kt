package org.totschnig.myexpenses.testutils

import android.content.ContentResolver
import android.content.Context
import android.content.pm.ProviderInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.test.IsolatedContext
import android.test.ProviderTestCase2
import android.test.RenamingDelegatingContext
import android.test.mock.MockContentResolver
import android.test.mock.MockContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.platform.app.InstrumentationRegistry
import org.mockito.Mockito
import org.totschnig.myexpenses.TestApp
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.findAccountType
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CASH
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.CurrencyFormatter
import java.io.File

open class BaseProviderTest : ProviderTestCase2<TransactionProvider>(
    TransactionProvider::class.java,
    TransactionProvider.AUTHORITY
) {
    private lateinit var transactionProvider: TransactionProvider
    private lateinit var targetContextWrapper: Context
    private lateinit var resolver: MockContentResolver

    val persistedPermissions = mutableSetOf<Uri>()

    val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    val app: TestApp
        get() = targetContext.applicationContext as TestApp

    val homeCurrency: CurrencyUnit
        get() = app.appComponent.currencyContext().homeCurrencyUnit

    val prefHandler: PrefHandler
        get() = app.appComponent.prefHandler()

    protected val repository: Repository
        get() = Repository(
            targetContextWrapper,
            Mockito.mock(CurrencyContext::class.java),
            Mockito.mock(CurrencyFormatter::class.java),
            prefHandler,
            Mockito.mock(DataStore::class.java) as DataStore<Preferences>
        )

    val contentResolver: ContentResolver
        get() = repository.contentResolver

    @JvmOverloads
    fun buildAccount(label: String, openingBalance: Long = 0L, syncAccountName: String? = null) =
        Account(
            label = label,
            currency = homeCurrency.code,
            openingBalance = openingBalance,
            syncAccountName = syncAccountName,
            type = cashAccount
        ).createIn(repository)

    fun deleteAccount(id: Long) {
        repository.deleteAccount(id)
    }

    val cashAccount: AccountType
        get() = repository.findAccountType(PREDEFINED_NAME_CASH)!!

    fun getTransactionFromDb(id: Long): Transaction? =
        Transaction.getInstanceFromDb(repository.contentResolver, id, homeCurrency)

    @Deprecated("Deprecated in Java")
    @Throws(Exception::class)
    override fun setUp() {
        transactionProvider = object : TransactionProvider() {
            override fun takePersistableUriPermission(uri: Uri) {
                persistedPermissions.add(uri)
            }

            override fun releasePersistableUriPermission(uri: Uri) {
                persistedPermissions.remove(uri)
            }
        }
        resolver = MockContentResolver()
        val filenamePrefix = "test."
        targetContextWrapper = RenamingDelegatingContext(
            DelegatedMockContext(resolver),  // The context that most methods are
            //delegated to
            context,  // The context that file methods are delegated to
            filenamePrefix
        )
        assertNotNull(transactionProvider)
        val providerInfo = ProviderInfo()
        providerInfo.authority = TransactionProvider.AUTHORITY
        transactionProvider.attachInfo(IsolatedContext(resolver, context), providerInfo)
        resolver.addProvider(TransactionProvider.AUTHORITY, transactionProvider)
    }

    @Deprecated("Deprecated in Java")
    @Throws(Exception::class)
    override fun tearDown() {
        //we need to skip super.tearDown(), since we do not call super.setUp
    }

    override fun getMockContentResolver() = resolver

    override fun getProvider() = transactionProvider

    private inner class DelegatedMockContext(val resolver: ContentResolver) : MockContext() {
        override fun createConfigurationContext(overrideConfiguration: Configuration) = this
        override fun getResources(): Resources = context.resources
        override fun getDir(name: String, mode: Int): File =
            context.getDir("mockContext2_$name", mode)

        override fun getApplicationContext() = this
        override fun getContentResolver() = resolver
    }
}