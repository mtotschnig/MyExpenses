package org.totschnig.myexpenses.test.espresso

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.*
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.testutils.BaseUiTest
import org.totschnig.myexpenses.testutils.childAtPosition
import kotlin.reflect.KClass

class SettingsTest : BaseUiTest<PreferenceActivity>() {
    @JvmField
    @Rule
    var scenarioRule = ActivityScenarioRule(
        PreferenceActivity::class.java
    )

    @Before
    fun initIntents() {
        testScenario = scenarioRule.scenario
        Intents.init()
    }

    @After
    fun releaseIntents() {
        Intents.release()
    }

    @Test
    fun manageCategories() {
        navigateTo(R.string.data, R.string.pref_manage_categories_title)
        intended(
            hasComponent(
                ManageCategories::class.java.name
            )
        )
        onView(withText(R.string.pref_manage_categories_title))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    private fun intended(clazz: KClass<*>) {
        intended(hasComponent(clazz.java.name)
        )
    }

    @Test
    fun manageMethods() {
        navigateTo(R.string.data, R.string.pref_manage_methods_title)
        intended(ManageMethods::class)
    }

    @Test
    fun importGrisbi() {
        navigateTo(R.string.menu_import, R.string.pref_import_from_grisbi_title)
        intended(GrisbiImport::class)
    }

    @Test
    fun importQif() {
        navigateTo(withText(R.string.menu_import), withText(getString(R.string.pref_import_title, "QIF")))
        intended(QifImport::class)
    }

    @Test
    fun importCsv() {
        navigateTo(withText(R.string.menu_import), withText(getString(R.string.pref_import_title, "CSV")))
        handleContribDialog(ContribFeature.CSV_IMPORT)
        intended(CsvImportActivity::class)
    }

    private val exportBackupTitle: String
        get() = getString(R.string.pref_category_title_export) + " / " + getString(R.string.menu_backup)

    @Test
    fun backup() {
        navigateTo(withText(exportBackupTitle), withText(R.string.pref_backup_summary))
        intended(BackupRestoreActivity::class)
        onView(withText(R.string.menu_backup))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun restore() {
        navigateTo(withText(exportBackupTitle), withText(R.string.pref_restore_title))
        intended(BackupRestoreActivity::class)
        onView(withText(R.string.pref_restore_title))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun manageSync() {
        navigateTo(R.string.synchronization, R.string.pref_manage_sync_backends_title)
        intended(ManageSyncBackends::class)
    }

    @Test
    fun roadmap() {
        navigateTo(R.string.feedback, R.string.roadmap_vote)
        intended(RoadmapVoteActivity::class)
    }

    @Test
    fun manageCurrencies() {
        navigateTo(androidx.preference.R.string.expand_button_title, R.string.pref_custom_currency_title)
        intended(ManageCurrencies::class)
    }

    companion object {
        fun navigateTo(headerTextId: Int, detailTextId: Int) {
            navigateTo(withText(headerTextId), withText(detailTextId))
        }
        fun navigateTo(headerMatcher: Matcher<View>, detailMatcher: Matcher<View>) {
            onView(header)
                .perform(
                    actionOnItem<RecyclerView.ViewHolder>(
                        hasDescendant(headerMatcher),
                        click()
                    )
                )
            onView(detail)
                .perform(
                    actionOnItem<RecyclerView.ViewHolder>(
                        hasDescendant(detailMatcher),
                        click()
                    )
                )
        }
        private val header: Matcher<View>
            get() = recyclerViewMatcher(androidx.preference.R.id.preferences_header)

        private val detail: Matcher<View>
            get() = recyclerViewMatcher(androidx.preference.R.id.preferences_detail)

        private fun recyclerViewMatcher(fragmentId: Int) = childAtPosition(
            childAtPosition(
                childAtPosition(
                    withId(fragmentId),
                    0
                ), 0
            ), 0
        )
    }
}