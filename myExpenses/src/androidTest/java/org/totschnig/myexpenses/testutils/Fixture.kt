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
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.addAttachments
import org.totschnig.myexpenses.db2.createSplitTransaction
import org.totschnig.myexpenses.db2.createTemplate
import org.totschnig.myexpenses.db2.entities.Template
import org.totschnig.myexpenses.db2.entities.Transaction
import org.totschnig.myexpenses.db2.findAccountType
import org.totschnig.myexpenses.db2.findCategory
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.db2.insertTransfer
import org.totschnig.myexpenses.db2.requireParty
import org.totschnig.myexpenses.db2.saveTagsForTransaction
import org.totschnig.myexpenses.db2.setGrouping
import org.totschnig.myexpenses.db2.storeExchangeRate
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.PREDEFINED_NAME_BANK
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CASH
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CCARD
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.generateUuid
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.myApplication
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ONE_TIME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR
import org.totschnig.myexpenses.provider.INVALID_CALENDAR_ID
import org.totschnig.myexpenses.provider.PlannerUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.viewmodel.data.Budget
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ThreadLocalRandom
import org.totschnig.myexpenses.test.R as RT

fun Repository.addDebugAttachment(transactionId: Long) {
    addAttachments(
        transactionId,
        listOf(Uri.parse("file:///android_asset/screenshot.jpg"))
    )
}

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
        val offset = LocalDateTime.now().minusMinutes(1)
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
            val offset = offset.minusDays(it.toLong())
            //Transaction 1
            val builder = TransactionBuilder()
                .accountId(account1.id)
                .amount(-random(12000))
                .catId(
                    findCat(
                        RT.string.testData_transaction1SubCat,
                        mainCat1
                    )
                )
                .date(offset)
            val transaction = builder.persist()
            if (withPicture) {
                repository.addDebugAttachment(transaction)
            }

            //Transaction 2
            TransactionBuilder()
                .accountId(account1.id)
                .amount(-random(2200L))
                .catId(
                    findCat(
                        RT.string.testData_transaction2SubCat,
                        mainCat2
                    )
                )
                .date(offset.minusHours(3))
                .comment(appContext.getString(R.string.testData_transaction2Comment))
                .persist()

            TransactionBuilder()
                .accountId(account1.id)
                .amount(-random(2500L))
                .catId(
                    findCat(
                        RT.string.testData_transaction3SubCat,
                        mainCat3
                    )
                )
                .date(offset.minusHours(6))
                .persist()

            TransactionBuilder()
                .accountId(account1.id)
                .amount(-random(5000L))
                .catId(
                    findCat(
                        RT.string.testData_transaction4SubCat,
                        mainCat2
                    )
                )
                .payee(appContext.getString(R.string.testData_transaction4Payee))
                .date(offset.minusHours(9))
                .persist()

            TransactionBuilder()
                .accountId(account1.id)
                .amount(-random(10000L))
                .date(offset.minusHours(12))
                .payee(johnDoe)
                .persist()

            TransactionBuilder()
                .accountId(account1.id)
                .amount(-10000L)
                .catId(mainCat6)
                .date(offset.minusHours(15))
                .persist()

            //Salary
            TransactionBuilder()
                .accountId(account3.id)
                .amount(200000)
                .date(offset.minusHours(18))
                .persist()

            //Transfer
            repository.insertTransfer(
                accountId = account1.id, transferAccountId = account3.id, amount = 25000, date = offset
            )
        }

        //Second account foreign Currency
        TransactionBuilder()
            .accountId(account2.id)
            .amount(-random(34567))
            .date(offset)
            .persist()

        //Transaction 8: Split
        val split = repository.createSplitTransaction(
            Transaction(
                accountId = account1.id,
                amount = -8967L,
                categoryId = DatabaseConstants.SPLIT_CATID,
                uuid = generateUuid()
            ), listOf(
                Transaction(
                    accountId = account1.id,
                    amount = -4523L,
                    uuid = generateUuid()
                ),
                Transaction(
                    accountId = account1.id,
                    amount = -4444L,
                    uuid = generateUuid()
                )
            )
        )
        val label = appContext.getString(R.string.testData_tag_project)
        repository.saveTagsForTransaction(listOf(saveTag(label)), split.id)

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
        val templateSubCat = testContext.getString(RT.string.testData_templateSubCat)
        val template = Template(
            accountId = account3.id,
            title = templateSubCat,
            amount = -90000L,
            payeeId = repository.requireParty(johnDoe),
            categoryId = findCat(
                templateSubCat,
                findCat(
                    testContext.getString(RT.string.testData_templateMainCat),
                    null
                )
            ),
            uuid = generateUuid()
        )
        val plan = Plan(
            LocalDate.now(),
            "FREQ=WEEKLY;COUNT=10;WKST=SU",
            template.title,
            "Description"
        )
        planId = ContentUris.parseId(
            plan.save(contentResolver, plannerUtils)!!
        )
        repository.createTemplate(
            template.copy(planId = planId)
        )
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
        private var amount: Long = 0
        private var catId: Long? = null
        private var date: LocalDateTime? = null
        private var crStatus: CrStatus? = null
        private var comment: String? = null
        private var payee: String? = null
        fun accountId(accountId: Long): TransactionBuilder {
            this.accountId = accountId
            return this
        }

        fun amount(amountMinor: Long): TransactionBuilder {
            amount = amountMinor
            return this
        }

        fun catId(catId: Long): TransactionBuilder {
            this.catId = catId
            return this
        }

        fun date(date: LocalDateTime): TransactionBuilder {
            this.date = date
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

        fun persist() = repository.insertTransaction(
            accountId = accountId,
            amount = amount,
            categoryId = catId,
            date = date ?: LocalDateTime.now(),
            crStatus = crStatus ?: CrStatus.UNRECONCILED,
            payeeId = payee?.let { repository.requireParty(it) },
            comment = comment,
            parentId = parentId
        ).id
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