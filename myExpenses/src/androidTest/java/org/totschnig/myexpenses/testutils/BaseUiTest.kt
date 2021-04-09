package org.totschnig.myexpenses.testutils

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.ListView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Matcher
import org.junit.Assert
import org.junit.Before
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.TestApp
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefKey
import se.emilsjolander.stickylistheaders.StickyListHeadersListView
import java.util.*
import java.util.concurrent.TimeoutException

abstract class BaseUiTest {
    private var isLarge = false

    val testContext: Context
        get() = InstrumentationRegistry.getInstrumentation().context

    val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    val app: TestApp
        get() = targetContext.applicationContext as TestApp

    @Before
    fun setUp() {
        isLarge = testContext.resources.getBoolean(org.totschnig.myexpenses.debug.test.R.bool.isLarge)
    }

    protected fun closeKeyboardAndSave() {
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        androidx.test.espresso.Espresso.onView(ViewMatchers.withId(R.id.CREATE_COMMAND)).perform(ViewActions.click())
    }

    protected val wrappedList: Matcher<View>
        get() = org.hamcrest.Matchers.allOf(
                ViewMatchers.isAssignableFrom(AdapterView::class.java),
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.list)),
                ViewMatchers.isDisplayed())

    /**
     * @param menuItemId id of menu item rendered in CAB on Honeycomb and higher
     * Click on a menu item, that might be visible or hidden in overflow menu
     */
    @JvmOverloads
    protected fun clickMenuItem(menuItemId: Int, isCab: Boolean = false) {
        try {
            val viewInteraction = androidx.test.espresso.Espresso.onView(ViewMatchers.withId(menuItemId))
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
            androidx.test.espresso.Espresso.onData(Matchers.menuIdMatcher(menuItemId)).inRoot(RootMatchers.isPlatformPopup()).perform(ViewActions.click())
        }
    }

    //https://stackoverflow.com/a/41415288/1199911
    private val currentActivity: Activity?
        get() {
            val activity = arrayOfNulls<Activity>(1)
            androidx.test.espresso.Espresso.onView(ViewMatchers.isRoot()).check { view: View, _: NoMatchingViewException? -> activity[0] = view.findViewById<View>(android.R.id.content).context as Activity }
            return activity[0]
        }

    protected fun handleContribDialog(contribFeature: ContribFeature?) {
        if (!app.appComponent.licenceHandler().hasAccessTo(contribFeature!!)) {
            try {
                //without play service a billing setup error dialog is displayed
                androidx.test.espresso.Espresso.onView(ViewMatchers.withText(android.R.string.ok)).perform(ViewActions.click())
            } catch (ignored: Exception) {
            }
            androidx.test.espresso.Espresso.onView(ViewMatchers.withText(R.string.dialog_title_contrib_feature)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            androidx.test.espresso.Espresso.onView(ViewMatchers.withText(R.string.dialog_contrib_no)).perform(ViewActions.scrollTo(), ViewActions.click())
        }
    }

    protected abstract val testScenario: ActivityScenario<out ProtectedFragmentActivity>

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

    private val list: ViewGroup?
        get() {
            var result: ViewGroup? = null
            testScenario.onActivity {
                result = it.currentFragment?.view?.findViewById(listId)
            }
            return result
        }

    protected open val listId: Int
        get() = R.id.list

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

    private val adapter: Adapter?
        get() {
            val list = list ?: return null
            if (list is StickyListHeadersListView) {
                return list.adapter
            }
            return if (list is ListView) {
                list.adapter
            } else null
        }

    @Throws(TimeoutException::class)
    protected fun waitForAdapter(): Adapter {
        var iterations = 0
        while (true) {
            val adapter = adapter
            try {
                Thread.sleep(500)
            } catch (ignored: InterruptedException) {
            }
            if (adapter != null) {
                return adapter
            }
            iterations++
            if (iterations > 10) throw TimeoutException()
        }
    }

    @Throws(TimeoutException::class)
    protected fun waitForSnackbarDismissed() {
        var iterations = 0
        while (true) {
            try {
                androidx.test.espresso.Espresso.onView(ViewMatchers.withId(com.google.android.material.R.id.snackbar_text))
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
        val instCtx = InstrumentationRegistry.getInstrumentation().context
        instCtx.resources.updateConfiguration(config,
                instCtx.resources.displayMetrics)
        app.settings?.edit()?.putString(PrefKey.UI_LANGUAGE.key, locale.language + "-" + locale.country)?.apply()
                ?: run {
                    Assert.fail("Could not find prefs")
                }
    }

    fun openCab() {
        onData(`is`(instanceOf(Cursor::class.java)))
                .inAdapterView(wrappedList)
                .atPosition(1)
                .perform(ViewActions.longClick())
    }
}