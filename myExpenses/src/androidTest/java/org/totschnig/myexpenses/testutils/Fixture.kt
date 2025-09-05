package org.totschnig.myexpenses.testutils

import android.annotation.SuppressLint
import android.app.Instrumentation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import com.google.common.truth.Truth
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.addAttachments
import org.totschnig.myexpenses.db2.findAccountType
import org.totschnig.myexpenses.db2.findCategory
import org.totschnig.myexpenses.db2.requireParty
import org.totschnig.myexpenses.db2.setGrouping
import org.totschnig.myexpenses.db2.storeExchangeRate
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.PREDEFINED_NAME_BANK
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CASH
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CCARD
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.SplitTransaction
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.myApplication
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ONE_TIME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_NONE
import org.totschnig.myexpenses.provider.INVALID_CALENDAR_ID
import org.totschnig.myexpenses.provider.PlannerUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.ui.DisplayParty
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.Tag
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.concurrent.ThreadLocalRandom
import org.totschnig.myexpenses.test.R as RT

@SuppressLint("InlinedApi")
class Fixture(inst: Instrumentation) {
    private val testContext: Context = inst.context
    private val appContext: MyApplication = inst.targetContext.myApplication
    lateinit var repository: Repository
    lateinit var account1: Account
        private set
    lateinit var account2: Account
        private set
    lateinit var account3: Account
        private set
    private lateinit var account4: Account
    private var budgetId: Long = 0L
    private var planId: Long = 0L

    val syncAccount1 by lazy {
        "Drive - " + appContext.getString(R.string.encrypted)
    }

    val syncAccount2 by lazy {
        "Dropbox - " + testContext.getString(RT.string.testData_sync_backend_2_name)
    }

    val syncAccount3 by lazy {
        "WebDAV - https://my.private.cloud/webdav/MyExpenses"
    }

    fun cleanup(contentResolver: ContentResolver) {
        Plan.delete(contentResolver, planId)
    }

    fun setup(
        withPicture: Boolean,
        repository: Repository,
        plannerUtils: PlannerUtils,
        defaultCurrency: CurrencyUnit
    ) {
        this.repository = repository
        val contentResolver = repository.contentResolver
        val foreignCurrency =
            appContext.appComponent.currencyContext()[if (defaultCurrency.code == "EUR") "GBP" else "EUR"]
        val exchangeRate = when(defaultCurrency.code) {
            "USD" -> 1.17 //eur to usd
            "EUR" -> 1.16 //GBP to eur
            "SAR" -> 4.38 //EUR to SAR
            "BRL" -> 6.31 //EUR to BRL
            "PLN" -> 4.26 //EUR to PLN
            "RUB" -> 94.02 //EUR to RUB
            "BGN" -> 1.96 //EUR to BGN
            "CZK" -> 24.56 //EUR to CZK
            "RON" -> 5.07 //EUR to RON
            "HUF" -> 396.63 //EUR to HUF
            "ILS" -> 3.89 //EUR to ILS
            "TRY" -> 47.92 //EUR to TRY
            "TWD" -> 35.61 //EUR to TWD
            "DKK" -> 7.46 //EUR to DKK
            "JPY" -> 171.48 // EUR to JPY
            "KHR" -> 4678.0 // EUR to KHR
            "INR" -> 102.25 // EUR to INR
            "KRW" ->  1618.06 // EUR to KRW
            "MYR" ->  4.92 // EUR to MYR
            "LKR" -> 352.92 // EUR to LKR
            "VND" -> 30786.00 // EUR to VND
            "CNY" ->  8.33 // EUR to CNY
            else -> 1.0
        }
        val accountTypeCash = repository.findAccountType(PREDEFINED_NAME_CASH)!!
        val accountTypeBank = repository.findAccountType(PREDEFINED_NAME_BANK)!!
        val accountTypeCard = repository.findAccountType(PREDEFINED_NAME_CCARD)!!
        account1 = Account(
            label = appContext.getString(R.string.testData_account1Label),
            currency = defaultCurrency.code,
            openingBalance = 90000,
            description = appContext.getString(R.string.testData_account1Description),
            type = accountTypeCash,
            syncAccountName = syncAccount1
        ).createIn(repository)
        repository.setGrouping(account1.id, Grouping.WEEK)
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
        account2 = Account(
            label = appContext.getString(R.string.testData_account2Label),
            currency = foreignCurrency.code,
            openingBalance = 50000,
            description = formatter.format(LocalDate.now()),
            type = accountTypeCash,
            color = testContext.resources.getColor(RT.color.material_red),
            syncAccountName = syncAccount2
        ).createIn(repository)
        repository.storeExchangeRate(
            account2.id,
            exchangeRate,
            account2.currency,
            defaultCurrency.code
        )
        account3 = Account(
            label = appContext.getString(R.string.testData_account3Label),
            currency = defaultCurrency.code,
            openingBalance = 200000,
            description = appContext.getString(R.string.testData_account3Description),
            type = accountTypeBank,
            color = testContext.resources.getColor(RT.color.material_blue),
            grouping = Grouping.DAY,
            syncAccountName = syncAccount3
        ).createIn(repository)
        account4 = Account(
            label = appContext.getString(R.string.testData_account3Description),
            currency = foreignCurrency.code,
            type = accountTypeCard,
            color = testContext.resources.getColor(RT.color.material_cyan)
        ).createIn(repository)

        val johnDoe = appContext.getString(R.string.testData_templatePayee)

        //set up categories
        setUpCategories(appContext.contentResolver)
        //set up transactions
        var offset = System.currentTimeMillis() - 1000
        //are used twice
        val mainCat1 = findCat(
            testContext.getString(RT.string.testData_transaction1MainCat),
            null
        )
        val mainCat2 = findCat(
            testContext.getString(RT.string.testData_transaction2MainCat),
            null
        )
        val mainCat3 = findCat(
            testContext.getString(RT.string.testData_transaction3MainCat),
            null
        )
        val mainCat6 = findCat(
            testContext.getString(RT.string.testData_transaction6MainCat),
            null
        )
        repeat(14) {
            //Transaction 1
            val builder = TransactionBuilder()
                .accountId(account1.id)
                .amount(defaultCurrency, -random(12000))
                .catId(
                    findCat(
                        RT.string.testData_transaction1SubCat,
                        mainCat1
                    )
                )
                .date(offset - 300000)
            val transaction = builder.persist()
            if (withPicture) {
                repository.addAttachments(transaction.id, listOf(Uri.parse("file:///android_asset/screenshot.jpg")))
            }

            //Transaction 2
            TransactionBuilder()
                .accountId(account1.id)
                .amount(defaultCurrency, -random(2200L))
                .catId(
                    findCat(
                        RT.string.testData_transaction2SubCat,
                        mainCat2
                    )
                )
                .date(offset - 7200000)
                .comment(appContext.getString(R.string.testData_transaction2Comment))
                .persist()

            TransactionBuilder()
                .accountId(account1.id)
                .amount(defaultCurrency, -random(2500L))
                .catId(
                    findCat(
                        RT.string.testData_transaction3SubCat,
                        mainCat3
                    )
                )
                .date(offset - 72230000)
                .persist()

            TransactionBuilder()
                .accountId(account1.id)
                .amount(defaultCurrency, -random(5000L))
                .catId(
                    findCat(
                        RT.string.testData_transaction4SubCat,
                        mainCat2
                    )
                )
                .payee(appContext.getString(R.string.testData_transaction4Payee))
                .date(offset - 98030000)
                .persist()

            TransactionBuilder()
                .accountId(account1.id)
                .amount(defaultCurrency, -random(10000L))
                .date(offset - 100390000)
                .payee(johnDoe)
                .persist()

            TransactionBuilder()
                .accountId(account1.id)
                .amount(defaultCurrency, -10000L)
                .catId(mainCat6)
                .date(offset - 210390000)
                .persist()

            //Salary
            TransactionBuilder()
                .accountId(account3.id)
                .amount(defaultCurrency, 200000)
                .date(offset - 100000)
                .persist()

            //Transfer
            val transfer = Transfer.getNewInstance(account1.id, defaultCurrency, account3.id)
            transfer.setAmount(Money(defaultCurrency, 25000L))
            transfer.setDate(Date(offset))
            transfer.save(contentResolver)
            offset -= 400000000
        }

        //Second account foreign Currency
        TransactionBuilder()
            .accountId(account2.id)
            .amount(foreignCurrency, -random(34567))
            .date(offset - 303900000)
            .persist()

        //Transaction 8: Split
        val split: Transaction = SplitTransaction.getNewInstance(contentResolver, account1.id, defaultCurrency, true)
        split.amount = Money(defaultCurrency, -8967L)
        split.status  = STATUS_NONE
        split.save(contentResolver, true)
        val label = appContext.getString(R.string.testData_tag_project)
        val tagList = listOf(Tag(saveTag(label), label, 0))
        split.saveTags(contentResolver, tagList)
        TransactionBuilder()
            .accountId(account1.id).parentId(split.id)
            .amount(defaultCurrency, -4523L)
            .catId(mainCat2)
            .persist()
        TransactionBuilder()
            .accountId(account1.id).parentId(split.id)
            .amount(defaultCurrency, -4444L)
            .catId(mainCat6)
            .persist()

        // Template
        Truth.assertWithMessage("Unable to create planner").that(
            plannerUtils.createPlanner(true)
        ).isNotEqualTo(INVALID_CALENDAR_ID)

        //createPlanner sets up a new plan, mPlannerCalendarId is only set in onSharedPreferenceChanged
        //if it is has not been called yet, when we save our plan, saving fails.
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        val template = Template.getTypedNewInstance(
            contentResolver,
            Transactions.TYPE_TRANSACTION,
            account3.id, defaultCurrency,
            false,
            null
        )!!
        template.amount = Money(defaultCurrency, -90000L)
        val templateSubCat =
            testContext.getString(RT.string.testData_templateSubCat)
        template.catId = findCat(
                templateSubCat,
                findCat(
                    testContext.getString(RT.string.testData_templateMainCat),
                    null
                )
            )
        template.title = templateSubCat
        template.party = DisplayParty(repository.requireParty(johnDoe), johnDoe)
        planId = ContentUris.parseId(
            Plan(
                LocalDate.now(),
                "FREQ=WEEKLY;COUNT=10;WKST=SU",
                template.title,
                template.compileDescription(appContext)
            )
                .save(contentResolver, plannerUtils)!!
        )
        template.planId = planId
        template.save(contentResolver)
            ?: throw RuntimeException("Could not save template")
        val budget = Budget(
            0L,
            account1.id,
            appContext.getString(R.string.testData_account1Description),
            "DESCRIPTION",
            defaultCurrency.code,
            Grouping.MONTH,
            -1,
            null as LocalDate?,
            null as LocalDate?,
            account1.label,
            true
        )
        budgetId = ContentUris.parseId(
            appContext.contentResolver.insert(
                TransactionProvider.BUDGETS_URI,
                budget.toContentValues(200000L)
            )!!
        )
        setCategoryBudget(budgetId, mainCat1, 50000)
        setCategoryBudget(budgetId, mainCat2, 40000)
        setCategoryBudget(budgetId, mainCat3, 30000)
        setCategoryBudget(budgetId, mainCat6, 20000)
    }

    private fun setCategoryBudget(budgetId: Long, categoryId: Long, amount: Long) {
        val result = appContext.contentResolver.update(
            BaseTransactionProvider.budgetAllocationUri(budgetId, categoryId),
            ContentValues().apply {
                put(KEY_BUDGET, amount)
                put(KEY_YEAR, LocalDate.now().year)
                put(KEY_SECOND_GROUP, 0)
                put(KEY_ONE_TIME, 0)
            }, null, null
        )
        Timber.d("Insert category budget: %d", result)
    }

    private fun findCat(resId: Int, parent: Long): Long {
        return findCat(testContext.getString(resId), parent)
    }

    private fun findCat(label: String, parent: Long?): Long {
        val result = repository.findCategory(label, parent)
        if (result == -1L) {
            throw RuntimeException("Could not find category")
        }
        return result
    }

    private fun random(n: Long): Long {
        return ThreadLocalRandom.current().nextLong(n)
    }

    private inner class TransactionBuilder {
        private var accountId: Long = 0
        private var parentId: Long? = null
        lateinit var amount: Money
        private var catId: Long? = null
        private var date: Date? = null
        private var crStatus: CrStatus? = null
        private var comment: String? = null
        private var payee: String? = null
        fun accountId(accountId: Long): TransactionBuilder {
            this.accountId = accountId
            return this
        }

        fun parentId(parentId: Long): TransactionBuilder {
            this.parentId = parentId
            return this
        }

        fun amount(currency: CurrencyUnit, amountMinor: Long): TransactionBuilder {
            amount = Money(currency, amountMinor)
            return this
        }

        fun catId(catId: Long): TransactionBuilder {
            this.catId = catId
            return this
        }

        fun date(date: Long): TransactionBuilder {
            this.date = Date(date)
            return this
        }

        fun crStatus(crStatus: CrStatus): TransactionBuilder {
            this.crStatus = crStatus
            return this
        }

        fun payee(payee: String): TransactionBuilder {
            this.payee = payee
            return this
        }

        fun comment(comment: String): TransactionBuilder {
            this.comment = comment
            return this
        }

        fun persist(): Transaction {
            val transaction = Transaction.getNewInstance(accountId, amount.currencyUnit)
            transaction.amount = amount
            transaction.catId = catId
            date?.let {
                transaction.setDate(it)
            }
            crStatus?.let {
                transaction.crStatus = it
            }
            transaction.party = payee?.let { DisplayParty(repository.requireParty(it), it) }
            transaction.comment = comment
            transaction.parentId = parentId
            transaction.save(repository.contentResolver)
            return transaction
        }
    }

    private fun saveTag(label: String?): Long {
        val values = ContentValues()
        values.put(KEY_LABEL, label)
        val uri = appContext.contentResolver.insert(TransactionProvider.TAGS_URI, values)
        return ContentUris.parseId(uri!!)
    }

    companion object {
        fun setUpCategories(contentResolver: ContentResolver) {
            val integerIntegerPair = contentResolver
                .call(
                    TransactionProvider.DUAL_URI,
                    TransactionProvider.METHOD_SETUP_CATEGORIES,
                    null,
                    null
                )!!.getSerializable(TransactionProvider.KEY_RESULT) as Pair<Int, Int>?
            Timber.d("Set up %d categories", integerIntegerPair!!.first)
        }
    }
}