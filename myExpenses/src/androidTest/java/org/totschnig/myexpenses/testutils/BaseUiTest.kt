package org.totschnig.myexpenses.testutils

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.internal.matcher.HelperMatchers.menuIdMatcher
import org.assertj.core.api.Assertions
import org.junit.Before
import org.mockito.Mockito
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.TestApp
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.myexpenses.viewmodel.data.Category
import java.util.*
import java.util.concurrent.TimeoutException
import org.totschnig.myexpenses.test.R as RT

abstract class BaseUiTest<A: ProtectedFragmentActivity> {
    private var isLarge = false

    val testContext: Context
        get() = InstrumentationRegistry.getInstrumentation().context

    val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    val app: TestApp
        get() = targetContext.applicationContext as TestApp

    val prefHandler: PrefHandler
        get() = app.appComponent.prefHandler()

    @Before
    fun setUp() {
        isLarge = testContext.resources.getBoolean(RT.bool.isLarge)
    }

    protected fun closeKeyboardAndSave() {
        closeSoftKeyboard()
        onView(ViewMatchers.withId(R.id.CREATE_COMMAND)).perform(ViewActions.click())
    }

    /**
     * @param menuItemId id of menu item rendered in CAB on Honeycomb and higher
     * Click on a menu item, that might be visible or hidden in overflow menu
     */
    @JvmOverloads
    protected fun clickMenuItem(menuItemId: Int, isCab: Boolean = false) {
        try {
            val viewInteraction = onView(ViewMatchers.withId(menuItemId))
            var searchInPlatformPopup = false
            try {
                searchInPlatformPopup = isCab && isLarge && app.packageManager.getActivityInfo(currentActivity!!.componentName, 0).themeResource == R.style.EditDialog
            } catch (ignored: PackageManager.NameNotFoundException) {
            }
            if (searchInPlatformPopup) {
                viewInteraction.inRoot(RootMatchers.isPlatformPopup())
            }
            viewInteraction.perform(ViewActions.click())
        } catch (e: NoMatchingViewException) {
            Espresso.openActionBarOverflowMenu(isCab)
            onData(menuIdMatcher(menuItemId)).inRoot(RootMatchers.isPlatformPopup()).perform(ViewActions.click())
        }
    }

    //https://stackoverflow.com/a/41415288/1199911
    private val currentActivity: Activity?
        get() {
            val activity = arrayOfNulls<Activity>(1)
            onView(ViewMatchers.isRoot()).check { view: View, _: NoMatchingViewException? -> activity[0] = view.findViewById<View>(android.R.id.content).context as Activity }
            return activity[0]
        }

    protected fun handleContribDialog(contribFeature: ContribFeature?) {
        if (!app.appComponent.licenceHandler().hasAccessTo(contribFeature!!)) {
            if (DistributionHelper.isPlay) {
                try {
                    //without play service a billing setup error dialog is displayed
                    onView(ViewMatchers.withText(android.R.string.ok)).perform(ViewActions.click())
                } catch (ignored: Exception) {
                }
            }
            onView(ViewMatchers.withText(R.string.dialog_title_contrib_feature)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            onView(ViewMatchers.withText(R.string.dialog_contrib_no)).perform(ViewActions.scrollTo(), ViewActions.click())
        }
    }

    lateinit var testScenario: ActivityScenario<A>

    protected fun rotate() {
        testScenario.onActivity {
            it.requestedOrientation = if (it.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    fun assertCanceled() {
        assertFinishing(Activity.RESULT_CANCELED)
    }

    @JvmOverloads
    fun assertFinishing(resultCode: Int = Activity.RESULT_OK) {
        Assertions.assertThat(testScenario.result.resultCode).isEqualTo(resultCode)
    }

    protected fun getQuantityString(resId: Int, @Suppress("SameParameterValue") quantity: Int, vararg formatArguments: Any): String {
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
            Mockito.mock(CurrencyContext::class.java),
            Mockito.mock(CurrencyFormatter::class.java),
            Mockito.mock(PrefHandler::class.java)
        )

    @Throws(TimeoutException::class)
    protected fun waitForSnackbarDismissed() {
        var iterations = 0
        while (true) {
            try {
                onView(ViewMatchers.withId(com.google.android.material.R.id.snackbar_text))
                        .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            } catch (e: Exception) {
                return
            }
            try {
                Thread.sleep(500)
            } catch (ignored: InterruptedException) {
            }
            iterations++
            if (iterations > 10) throw TimeoutException()
        }
    }

    protected fun configureLocale(locale: Locale) {
        Locale.setDefault(locale)
        val config = Configuration()
        config.locale = locale
        testContext.resources.update(config)
        targetContext.applicationContext.resources.update(config)
        prefHandler.putString(PrefKey.UI_LANGUAGE, locale.language + "-" + locale.country)
    }

    private fun Resources.update(configuration: Configuration) {
        updateConfiguration(configuration, displayMetrics)
    }

    protected fun writeCategory(label: String, parentId: Long? = null) =
        ContentUris.parseId(repository.saveCategory(Category(label = label, parentId = parentId))!!)
}