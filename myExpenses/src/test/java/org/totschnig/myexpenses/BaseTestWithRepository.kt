package org.totschnig.myexpenses

import android.content.ContentResolver
import android.content.ContentUris
import androidx.test.core.app.ApplicationProvider
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.createAccount
import org.totschnig.myexpenses.db2.findAccountType
import org.totschnig.myexpenses.db2.saveCategory
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CASH
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.model2.Category
import org.totschnig.myexpenses.provider.BudgetInfo
import org.totschnig.myexpenses.provider.TemplateInfo
import org.totschnig.myexpenses.provider.TransactionProvider

abstract class BaseTestWithRepository {

    val application: MyApplication = ApplicationProvider.getApplicationContext<MyApplication>()

    val currencyContext = application.appComponent.currencyContext()

    val prefHandler = application.appComponent.prefHandler()

    val dataStore = application.appComponent.preferencesDataStore()

    val exchangeRateHandler = application.appComponent.exchangeRateHandler()

    val repository: Repository = Repository(
        application,
        currencyContext,
        prefHandler,
        dataStore
    )

    val contentResolver: ContentResolver = repository.contentResolver

    fun writeCategory(
        label: String,
        parentId: Long? = null,
        uuid: String? = null,
        type: Byte = FLAG_NEUTRAL,
        icon: String? = null
    ) =
        repository.saveCategory(
            Category(
                label = label,
                parentId = parentId,
                uuid = uuid,
                type = type,
                icon = icon
            )
        )!!

    protected fun insertTemplate(
        accountId: Long,
        title: String,
        amount: Long,
        categoryId: Long? = null,
        payeeId: Long? = null
    ) = ContentUris.parseId(
        contentResolver.insert(
            TransactionProvider.TEMPLATES_URI, TemplateInfo(
                accountId = accountId,
                amount = amount,
                title = title,
                catId = categoryId,
                payeeId = payeeId
            ).contentValues
        )!!
    )

    protected fun insertBudget(
        accountId: Long,
        title: String,
        amount: Long,
        grouping: Grouping = Grouping.MONTH
    ) = ContentUris.parseId(
        contentResolver.insert(
            TransactionProvider.BUDGETS_URI, BudgetInfo(
                accountId = accountId,
                title = title,
                amount = amount,
                grouping = grouping
            ).contentValues
        )!!
    )

    protected fun insertAccount(
        label: String,
        openingBalance: Long = 0,
        accountType: String = PREDEFINED_NAME_CASH,
        currency: String = currencyContext.homeCurrencyString,
        dynamic: Boolean = false,
        description: String = "My account of type $accountType",
        syncAccountName: String? = null
    ) = repository.createAccount(
        Account(
            label = label,
            type = repository.findAccountType(accountType)!!,
            openingBalance = openingBalance,
            currency = currency,
            dynamicExchangeRates = dynamic,
            description = description,
            syncAccountName = syncAccountName
        )
    ).id
}