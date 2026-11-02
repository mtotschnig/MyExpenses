package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.Action
import org.totschnig.myexpenses.activity.ManageCategories
import org.totschnig.myexpenses.compose.TEST_TAG_EDIT_TEXT
import org.totschnig.myexpenses.compose.TEST_TAG_POSITIVE_BUTTON
import org.totschnig.myexpenses.db2.deleteAllCategories
import org.totschnig.myexpenses.provider.KEY_PARENTID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.testutils.BaseComposeTest
import org.totschnig.myexpenses.testutils.Espresso
import org.totschnig.myexpenses.testutils.TestShard3

@TestShard3
class ManageCategoriesTest : BaseComposeTest<ManageCategories>() {
    @get:Rule
    var scenarioRule = ActivityScenarioRule<ManageCategories>(
        Intent(ApplicationProvider.getApplicationContext(), ManageCategories::class.java).apply {
            action = Action.MANAGE.name
        }
    )

    @Before
    fun setup() {
        testScenario = scenarioRule.scenario
    }

    @After
    fun cleanup() {
        repository.deleteAllCategories()
    }

    @Test
    fun setupCategoriesShouldPopulateList() {
        val origDataSize = repository.count(TransactionProvider.CATEGORIES_URI)
        Espresso.openActionBarOverflowMenu()
        onView(withText(R.string.menu_categories_setup_default))
            .perform(click())
        composeTestRule.onNodeWithText(getString(R.string.menu_import)).performClick()
        assertThat(repository.count(TransactionProvider.CATEGORIES_URI)).isGreaterThan(origDataSize)
    }

    @Test
    fun shouldCreateMainCategory() {
        clickFab()
        val origCategoryCount = repository.count(TransactionProvider.CATEGORIES_URI)
        composeTestRule.onNodeWithTag(TEST_TAG_EDIT_TEXT)
            .performTextInput("Main category")
        composeTestRule.onNodeWithTag(TEST_TAG_POSITIVE_BUTTON).performClick()
        assertThat(repository.count(TransactionProvider.CATEGORIES_URI))
            .isEqualTo(1 + origCategoryCount)
    }

    @Test
    fun shouldCreateSubCategoryFromParentDropdown() {
        val parentId = writeCategory(label = "Parent Category")
        clickFab()
        composeTestRule.onNodeWithTag(TEST_TAG_EDIT_TEXT)
            .performTextInput("Sub category")
        composeTestRule.onNodeWithText(getString(R.string.category_type_main)).performClick()
        composeTestRule.onNode(hasAnyAncestor(isPopup()) and hasText("Parent Category")).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_POSITIVE_BUTTON).performClick()
        assertThat(
            repository.count(
                TransactionProvider.CATEGORIES_URI,
                "$KEY_PARENTID = ?",
                arrayOf(parentId.toString()),
            )
        ).isEqualTo(1)
    }

    @Test
    fun shouldChangeParentOfExistingCategory() {
        val parentId = writeCategory(label = "Parent Category")
        writeCategory(label = "Sub Category")
        clickContextItem(R.string.menu_edit, composeTestRule.onNodeWithText("Sub Category"))
        composeTestRule.onNodeWithText(getString(R.string.category_type_main)).performClick()
        composeTestRule.onNode(hasAnyAncestor(isPopup()) and hasText("Parent Category")).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_POSITIVE_BUTTON).performClick()
        assertThat(
            repository.count(
                TransactionProvider.CATEGORIES_URI,
                "$KEY_PARENTID = ?",
                arrayOf(parentId.toString()),
            )
        ).isEqualTo(1)
    }
}