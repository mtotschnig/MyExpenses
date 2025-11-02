package org.totschnig.myexpenses.testutils

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.widget.MenuPopupWindow.MenuDropDownListView
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions
import com.adevinta.android.barista.interaction.BaristaScrollInteractions
import com.adevinta.android.barista.internal.matcher.HelperMatchers.menuIdMatcher
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matchers.not
import org.junit.Assume
import org.junit.Before
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.TestApp
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.RepositoryTemplate
import org.totschnig.myexpenses.db2.createSplitTemplate
import org.totschnig.myexpenses.db2.createSplitTransaction
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.entities.Template
import org.totschnig.myexpenses.db2.entities.Transaction
import org.totschnig.myexpenses.db2.findAccountType
import org.totschnig.myexpenses.db2.loadAttachments
import org.totschnig.myexpenses.db2.loadTagsForTemplate
import org.totschnig.myexpenses.db2.loadTemplate
import org.totschnig.myexpenses.db2.loadTransaction
import org.totschnig.myexpenses.db2.saveCategory
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Model.generateUuid
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.model2.Account.Companion.DEFAULT_COLOR
import org.totschnig.myexpenses.model2.Category
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.CalendarProviderProxy
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.PlannerUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.TEMPLATES_URI
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert
import java.time.LocalDate
import java.util.concurrent.TimeoutException
import org.totschnig.myexpenses.test.R as RT

data class TransactionInfo(
    val accountId: Long,
    val amount: Long,
    val category: Long? = null,
    val party: Long? = null,
    val tags: List<Long> = emptyList(),
    val splitParts: List<TransactionInfo>? = null,
    val attachments: List<Uri> = emptyList(),
    val debtId: Long? = null
)

abstract class BaseUiTest<A : ProtectedFragmentActivity> {
    private var isLarge = false

    val testContext: Context
        get() = getInstrumentation().context

    val targetContext: Context
        get() = getInstrumentation().targetContext

    val app: TestApp
        get() = targetContext.applicationContext as TestApp

    val prefHandler: PrefHandler
        get() = app.appComponent.prefHandler()

    val dataStore: DataStore<Preferences>
        get() = app.appComponent.preferencesDataStore()

    val plannerUtils: PlannerUtils
        get() = app.appComponent.plannerUtils()

    val currencyContext: CurrencyContext
        get() = app.appComponent.currencyContext()


    val homeCurrency: CurrencyUnit by lazy { currencyContext.homeCurrencyUnit }

    @JvmOverloads
    fun buildAccount(
        label: String,
        openingBalance: Long = 0L,
        currency: String = homeCurrency.code,
        excludeFromTotals: Boolean = false,
        dynamicExchangeRates: Boolean = false,
        type: AccountType = AccountType.CASH,
        color: Int = DEFAULT_COLOR,
    ) =
        Account(
            label = label,
            openingBalance = openingBalance,
            currency = currency,
            excludeFromTotals = excludeFromTotals,
            dynamicExchangeRates = dynamicExchangeRates,
            type = repository.findAccountType(type.name)!!,
            color = color
        ).createIn(repository)

    fun deleteAccount(label: String) {
        val accountId = contentResolver.query(
            TransactionProvider.ACCOUNTS_URI,
            arrayOf(KEY_ROWID),
            "$KEY_LABEL = ?",
            arrayOf(label),
            null
        )!!.use {
            it.moveToFirst()
            it.getLong(0)
        }
        repository.deleteAccount(accountId)
    }

    fun getTransactionFromDb(id: Long): Transaction = repository.loadTransaction(id).data

    @Before
    fun setUp() {
        isLarge = testContext.resources.getBoolean(RT.bool.isLarge)
    }

    protected fun closeKeyboardAndSave() {
        closeSoftKeyboard()
        clickFab()
    }

    fun typeToAndCloseKeyBoard(@IdRes editTextId: Int, text: String) {
        BaristaScrollInteractions.safelyScrollTo(editTextId)
        BaristaEditTextInteractions.typeTo(editTextId, text)
        closeSoftKeyboard()
    }

    /**
     * @param menuItemId id of menu item rendered in CAB on Honeycomb and higher
     * Click on a menu item, that might be visible or hidden in overflow menu
     */
    protected fun clickMenuItem(@IdRes menuItemId: Int, isCab: Boolean = false) {
        try {
            onView(withId(menuItemId)).apply {
                if (try {
                        isCab && isLarge && app.packageManager.getActivityInfo(
                            currentActivity!!.componentName,
                            0
                        ).themeResource == R.style.EditDialog
                    } catch (_: PackageManager.NameNotFoundException) {
                        false
                    }
                ) {
                    inRoot(RootMatchers.isPlatformPopup())
                }
            }.perform(click())
        } catch (_: NoMatchingViewException) {
            Espresso.openActionBarOverflowMenu(isCab)
            onData(menuIdMatcher(menuItemId)).inRoot(RootMatchers.isPlatformPopup())
                .perform(click())
        }
    }

    protected fun assertOverflowItemChecked(@IdRes menuItemId: Int, checked: Boolean) {
        Espresso.openActionBarOverflowMenu()
        onData(menuIdMatcher(menuItemId)).inRoot(RootMatchers.isPlatformPopup())
            .check(
                matches(
                    hasDescendant(if (checked) isChecked() else isNotChecked()))
                )
        pressBack()
    }

    protected fun assertMenuItemHidden(@IdRes menuItemId: Int, isCab: Boolean = false) {
        onView(withId(menuItemId)).apply {
            if (try {
                    isCab && isLarge && app.packageManager.getActivityInfo(
                        currentActivity!!.componentName,
                        0
                    ).themeResource == R.style.EditDialog
                } catch (_: PackageManager.NameNotFoundException) {
                    false
                }
            ) {
                inRoot(RootMatchers.isPlatformPopup())
            }
        }.check(doesNotExist())
        Espresso.openActionBarOverflowMenu(isCab)
        onView(isAssignableFrom(MenuDropDownListView::class.java))
            .check(matches(not(withAdaptedData(menuIdMatcher(menuItemId)))))
        pressBack()
    }

    //https://stackoverflow.com/a/41415288/1199911
    private val currentActivity: Activity?
        get() {
            val activity = arrayOfNulls<Activity>(1)
            onView(ViewMatchers.isRoot()).check { view: View, _: NoMatchingViewException? ->
                activity[0] = view.findViewById<View>(android.R.id.content).context as Activity
            }
            return activity[0]
        }

    protected fun handleContribDialog(contribFeature: ContribFeature?) {
        if (!app.appComponent.licenceHandler().hasAccessTo(contribFeature!!)) {
            if (DistributionHelper.isPlay) {
                try {
                    //without play service a billing setup error dialog is displayed
                    onView(ViewMatchers.withText(android.R.string.ok)).perform(click())
                } catch (_: Exception) {
                }
            }
            onView(ViewMatchers.withSubstring(getString(R.string.dialog_title_contrib_feature))).check(
                matches(isDisplayed())
            )
            onView(ViewMatchers.withText(R.string.button_try)).perform(scrollTo(), click())
        }
    }

    lateinit var testScenario: ActivityScenario<A>

    protected fun doWithRotation(actions: () -> Unit) {
        val device = UiDevice.getInstance(getInstrumentation())
        Assume.assumeTrue(device.isNaturalOrientation)
        device.setOrientationRight()
        try {
            //without the sleep, actions might run before the orientation change
            Thread.sleep(500)
            actions()
        } finally {
            device.setOrientationNatural()
        }
    }

    fun assertCanceled() {
        assertFinishing(Activity.RESULT_CANCELED)
    }

    @JvmOverloads
    fun assertFinishing(resultCode: Int = Activity.RESULT_OK) {
        assertThat(testScenario.result.resultCode).isEqualTo(resultCode)
    }

    protected fun getQuantityString(
        resId: Int,
        @Suppress("SameParameterValue") quantity: Int,
        vararg formatArguments: Any,
    ): String {
        var result: String? = null
        testScenario.onActivity {
            result = it.resources.getQuantityString(resId, quantity, *formatArguments)
        }
        return result!!
    }

    protected fun getString(resId: Int, vararg formatArguments: Any): String {
        var result: String? = null
        testScenario.onActivity {
            result = it.getString(resId, *formatArguments)
        }
        return result!!
    }

    protected val repository: Repository
        get() = Repository(
            ApplicationProvider.getApplicationContext<MyApplication>(),
            currencyContext,
            prefHandler,
            dataStore
        )

    val contentResolver: ContentResolver = repository.contentResolver

    @Throws(TimeoutException::class)
    protected fun waitForSnackbarDismissed() {
        var iterations = 0
        while (true) {
            try {
                onView(withId(com.google.android.material.R.id.snackbar_text))
                    .check(matches(isDisplayed()))
            } catch (_: Exception) {
                return
            }
            try {
                Thread.sleep(500)
            } catch (_: InterruptedException) {
            }
            iterations++
            if (iterations > 10) throw TimeoutException()
        }
    }

    protected fun writeCategory(label: String, parentId: Long? = null, type: Byte = FLAG_EXPENSE, icon: String? = null) =
        repository.saveCategory(Category(label = label, parentId = parentId, type = type, icon = icon))!!

    fun unlock() {
        (app.appComponent.licenceHandler() as MockLicenceHandler).setLockState(false)
    }

    protected fun prepareSplit(accountId: Long) = repository.createSplitTransaction(
        Transaction(accountId = accountId, amount = 10000, categoryId = DatabaseConstants.SPLIT_CATID, uuid = generateUuid()),
        listOf(
            Transaction(accountId = accountId, amount = 5000, uuid = generateUuid()),
            Transaction(accountId = accountId, amount = 5000, uuid = generateUuid())
        )
    ).id

    protected fun prepareSplitTemplate(accountId: Long)  = repository.createSplitTemplate(
        Template(title = TEMPLATE_TITLE, accountId = accountId, amount = 10000, categoryId = DatabaseConstants.SPLIT_CATID, uuid = generateUuid()),
        listOf(
            Template(accountId = accountId, amount = 5000, title = "", uuid = generateUuid()),
            Template(accountId = accountId, amount = 5000, title = "", uuid = generateUuid())
        )
    ).id

    fun clickFab() {
        onView(withId(R.id.fab)).perform(click())
    }

    fun checkAccount(label: String) {
        onView(withId(R.id.Account)).check(matches(withSpinnerText(containsString(label))))
    }

    fun setAccount(label: String) {
        onView(withId(R.id.Account)).perform(scrollTo(), click())
        onData(withAccountGrouped(label))
            .perform(click())
    }

    protected fun assertTransaction(
        id: Long,
        expectedTransaction: TransactionInfo
    ) {
        val (expectedAccount, expectedAmount, expectedCategory, expectedParty, expectedTags, expectedSplitParts, expectedAttachments) =
            expectedTransaction

        val transaction = repository.loadTransaction(id)
        val attachments = repository.loadAttachments(id)

        with(transaction.data) {
            assertThat(amount).isEqualTo(expectedAmount)
            assertThat(accountId).isEqualTo(expectedAccount)
            assertThat(categoryId).isEqualTo(expectedCategory)
            assertThat(payeeId).isEqualTo(expectedParty)
        }
        assertThat(transaction.data.tagList).containsExactlyElementsIn(expectedTags)
        assertThat(attachments).containsExactlyElementsIn(expectedAttachments)

        if (expectedSplitParts == null) {
            assertThat(transaction.splitParts).isNull()
        } else {
            assertThat(transaction.splitParts).isNotNull()
            assertThat(transaction.splitParts!!.size).isEqualTo(expectedSplitParts.size)
            // 2. Map the actual split parts into the same data structure as the expected parts.
            val actualSplitPartsAsInfo = transaction.splitParts.map { actualPart ->
                TransactionInfo(
                    accountId = actualPart.data.accountId,
                    amount = actualPart.data.amount,
                    category = actualPart.data.categoryId,
                    tags = actualPart.data.tagList,
                    debtId = actualPart.data.debtId
                )
            }
            assertThat(actualSplitPartsAsInfo).containsExactlyElementsIn(expectedSplitParts)
        }
    }

    protected fun assertTemplate(
        expectedAccount: Long,
        expectedAmount: Long,
        templateTitle: String = TEMPLATE_TITLE,
        expectedTags: List<String> = emptyList(),
        expectedSplitParts: List<Long>? = null,
        expectedCategory: Long? = null,
        expectedParty: Long? = null,
        expectedPlanRecurrence: Plan.Recurrence = Plan.Recurrence.NONE,
        checkPlanInstance: Boolean = false
    ): RepositoryTemplate {
        val templateId = contentResolver.query(
            TEMPLATES_URI,
            arrayOf(KEY_ROWID),
            "$KEY_TITLE = ?",
            arrayOf(templateTitle),
            null
        )!!.use {
            assertWithMessage("No template with title $templateTitle").that(it.moveToFirst())
                .isTrue()
            it.getLong(0)
        }
        val template = repository.loadTemplate(templateId)!!
        val tags = repository.loadTagsForTemplate(templateId)
        with(template.data) {
            assertThat(amount).isEqualTo(expectedAmount)
            assertThat(title).isEqualTo(templateTitle)
            assertThat(accountId).isEqualTo(expectedAccount)
            assertThat(categoryId).isEqualTo(expectedCategory)
            assertThat(payeeId).isEqualTo(expectedParty)
        }
        assertThat(tags.map { it.label }).containsExactlyElementsIn(expectedTags)

        if (expectedSplitParts == null) {
            assertThat(template.splitParts).isNull()
        } else {
            assertThat(template.splitParts!!.map { it.data.amount })
                .containsExactlyElementsIn(expectedSplitParts)
        }

        if (expectedPlanRecurrence != Plan.Recurrence.NONE) {
            assertThat(template.plan!!.id).isGreaterThan(0)
            if (expectedPlanRecurrence != Plan.Recurrence.CUSTOM) {
                val today = LocalDate.now()
                assertThat(template.plan.rRule).isEqualTo(expectedPlanRecurrence.toRule(today))
            }
        } else {
            assertThat(template.plan).isNull()
        }
        if (checkPlanInstance) {
            contentResolver.query(
                TransactionProvider.PLAN_INSTANCE_SINGLE_URI(
                    template.id,
                    CalendarProviderProxy.calculateId(template.plan!!.dtStart)
                ),
                null, null, null, null
            ).useAndAssert {
                hasCount(1)
                movesToFirst()
                hasLong(KEY_TRANSACTIONID) { isGreaterThan(0) }
            }
        }
        return template
    }
}