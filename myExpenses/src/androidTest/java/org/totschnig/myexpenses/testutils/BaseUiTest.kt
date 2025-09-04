package org.totschnig.myexpenses.testutils

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
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
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.findAccountType
import org.totschnig.myexpenses.db2.saveCategory
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CASH
import org.totschnig.myexpenses.model.SplitTransaction
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.model2.Category
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_NONE
import org.totschnig.myexpenses.provider.PlannerUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.DebugCurrencyFormatter
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import java.util.concurrent.TimeoutException
import org.totschnig.myexpenses.test.R as RT

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

    private val currencyContext: CurrencyContext
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
    ) =
        Account(
            label = label,
            openingBalance = openingBalance,
            currency = currency,
            excludeFromTotals = excludeFromTotals,
            dynamicExchangeRates = dynamicExchangeRates,
            type = repository.findAccountType(type.name)!!
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

    fun getTransactionFromDb(id: Long): Transaction =
        Transaction.getInstanceFromDb(contentResolver, id, homeCurrency)

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
            DebugCurrencyFormatter,
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

    protected fun writeCategory(label: String, parentId: Long? = null, type: Byte = FLAG_EXPENSE) =
        repository.saveCategory(Category(label = label, parentId = parentId, type = type))!!

    fun unlock() {
        (app.appComponent.licenceHandler() as MockLicenceHandler).setLockState(false)
    }

    protected fun prepareSplit(accountId: Long): Long {
        val currencyUnit = homeCurrency
        return with(SplitTransaction.getNewInstance(contentResolver, accountId, currencyUnit)) {
            amount = Money(currencyUnit, 10000)
            status = STATUS_NONE
            save(contentResolver, true)
            val part = Transaction.getNewInstance(accountId, currencyUnit, id)
            part.amount = Money(currencyUnit, 5000)
            part.save(contentResolver)
            part.amount = Money(currencyUnit, 5000)
            part.saveAsNew(contentResolver)
            id
        }
    }

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
}