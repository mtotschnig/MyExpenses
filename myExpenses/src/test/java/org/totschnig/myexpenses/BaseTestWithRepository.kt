package org.totschnig.myexpenses

import android.content.ContentResolver
import android.content.ContentUris
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.saveCategory
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model2.Category
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.BudgetInfo
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TemplateInfo
import org.totschnig.myexpenses.provider.TransactionInfo
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.CurrencyFormatter
import java.util.Currency

abstract class BaseTestWithRepository {
    val currencyContext: CurrencyContext =
        mock(CurrencyContext::class.java).also { currencyContext ->
            `when`(currencyContext[ArgumentMatchers.anyString()]).thenAnswer {
                CurrencyUnit(Currency.getInstance(it.getArgument(0) as String))
            }
            `when`(currencyContext.homeCurrencyString).thenReturn("EUR")
        }
    val repository: Repository = Repository(
        ApplicationProvider.getApplicationContext<MyApplication>(),
        currencyContext,
        mock(CurrencyFormatter::class.java),
        mock(PrefHandler::class.java),
        mock(DataStore::class.java) as DataStore<Preferences>
    )

    val contentResolver: ContentResolver = repository.contentResolver

    fun writeCategory(label: String, parentId: Long? = null) =
        repository.saveCategory(Category(label = label, parentId = parentId))!!

    protected fun insertTransaction(
        accountId: Long,
        amount: Long,
        parentId: Long? = null,
        categoryId: Long? = null,
        crStatus: CrStatus = CrStatus.UNRECONCILED
    ): Pair<Long, String> {
        val contentValues = TransactionInfo(
            accountId = accountId,
            amount = amount,
            catId = categoryId,
            crStatus = crStatus,
            parentId = parentId
        ).contentValues
        val id = ContentUris.parseId(contentResolver.insert(TransactionProvider.TRANSACTIONS_URI, contentValues)!!)
        return id to contentValues.getAsString(DatabaseConstants.KEY_UUID)
    }

    protected fun insertTemplate(
        accountId: Long,
        title: String,
        amount: Long,
        categoryId: Long? = null
    ) = ContentUris.parseId(contentResolver.insert(
        TransactionProvider.TEMPLATES_URI, TemplateInfo(
            accountId = accountId,
            amount = amount,
            title = title,
            catId = categoryId
        ).contentValues
    )!!)

    protected fun insertBudget(
        accountId: Long,
        title: String,
        amount: Long
    ) = ContentUris.parseId(contentResolver.insert(
        TransactionProvider.BUDGETS_URI, BudgetInfo(
            accountId = accountId,
            title = title,
            amount = amount,
            grouping = Grouping.MONTH
        ).contentValues
    )!!)
}