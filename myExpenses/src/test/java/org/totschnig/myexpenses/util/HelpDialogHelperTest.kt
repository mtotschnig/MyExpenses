package org.totschnig.myexpenses.util

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.totschnig.myexpenses.activity.AccountEdit
import org.totschnig.myexpenses.activity.BudgetActivity
import org.totschnig.myexpenses.activity.BudgetEdit
import org.totschnig.myexpenses.activity.CsvImportActivity
import org.totschnig.myexpenses.activity.DebtEdit
import org.totschnig.myexpenses.activity.DistributionActivity
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.HELP_VARIANT_MANGE
import org.totschnig.myexpenses.activity.HELP_VARIANT_MERGE_MODE
import org.totschnig.myexpenses.activity.HELP_VARIANT_PLANNER
import org.totschnig.myexpenses.activity.HELP_VARIANT_PLANS
import org.totschnig.myexpenses.activity.HELP_VARIANT_SELECT_FILTER
import org.totschnig.myexpenses.activity.HELP_VARIANT_SELECT_MAPPING
import org.totschnig.myexpenses.activity.HELP_VARIANT_SPLIT
import org.totschnig.myexpenses.activity.HELP_VARIANT_SPLIT_PART_CATEGORY
import org.totschnig.myexpenses.activity.HELP_VARIANT_SPLIT_PART_TRANSFER
import org.totschnig.myexpenses.activity.HELP_VARIANT_TEMPLATES
import org.totschnig.myexpenses.activity.HELP_VARIANT_TEMPLATE_CATEGORY
import org.totschnig.myexpenses.activity.HELP_VARIANT_TEMPLATE_SPLIT
import org.totschnig.myexpenses.activity.HELP_VARIANT_TEMPLATE_TRANSFER
import org.totschnig.myexpenses.activity.HELP_VARIANT_TRANSACTION
import org.totschnig.myexpenses.activity.HELP_VARIANT_TRANSFER
import org.totschnig.myexpenses.activity.HistoryActivity
import org.totschnig.myexpenses.activity.ManageBudgets
import org.totschnig.myexpenses.activity.ManageCategories
import org.totschnig.myexpenses.activity.ManageCurrencies
import org.totschnig.myexpenses.activity.ManageMethods
import org.totschnig.myexpenses.activity.ManageParties
import org.totschnig.myexpenses.activity.ManageStaleImages
import org.totschnig.myexpenses.activity.ManageSyncBackends
import org.totschnig.myexpenses.activity.ManageTags
import org.totschnig.myexpenses.activity.ManageTemplates
import org.totschnig.myexpenses.activity.MethodEdit
import org.totschnig.myexpenses.activity.MyExpenses
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.activity.RoadmapVoteActivity

@RunWith(ParameterizedRobolectricTestRunner::class)
class HelpDialogHelperTest(private val activity: Class<out ProtectedFragmentActivity>) :
    BaseHelpTest() {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun params() = listOf(
            arrayOf(AccountEdit::class.java),
            arrayOf(BudgetActivity::class.java),
            arrayOf(BudgetEdit::class.java),
            arrayOf(CsvImportActivity::class.java),
            arrayOf(DebtEdit::class.java),
            arrayOf(DistributionActivity::class.java),
            arrayOf(ExpenseEdit::class.java),
            arrayOf(HistoryActivity::class.java),
            arrayOf(ManageBudgets::class.java),
            arrayOf(ManageCategories::class.java),
            arrayOf(ManageCurrencies::class.java),
            arrayOf(ManageMethods::class.java),
            arrayOf(ManageParties::class.java),
            arrayOf(ManageStaleImages::class.java),
            arrayOf(ManageSyncBackends::class.java),
            arrayOf(ManageTags::class.java),
            arrayOf(ManageTemplates::class.java),
            arrayOf(MethodEdit::class.java),
            arrayOf(MyExpenses::class.java),
            arrayOf(RoadmapVoteActivity::class.java)
        )
    }

    @Test
    fun testHelpStringResExists() {
        var menuItemsIdentifier: Int
        val className = activity.simpleName
        assertThat(activity).isAssignableTo(ProtectedFragmentActivity::class.java)
        val titleIdentifier = getStringIdentifier("help_" + className + "_title")
        menuItemsIdentifier = getArrayIdentifier(className + "_menuitems")
        if (menuItemsIdentifier != 0) {
            testMenuItems(
                className,
                null,
                resources.getStringArray(menuItemsIdentifier),
                "menu"
            )
        }
        menuItemsIdentifier = getArrayIdentifier(className + "_cabitems")
        if (menuItemsIdentifier != 0) {
            testMenuItems(className, null, resources.getStringArray(menuItemsIdentifier), "cab")
        }
        menuItemsIdentifier = getArrayIdentifier(className + "_formfields")
        if (menuItemsIdentifier != 0) {
            testMenuItems(
                className,
                null,
                resources.getStringArray(menuItemsIdentifier),
                "form"
            )
        }
        val helpVariants = getHelpVariants(activity)
        if (helpVariants.isEmpty()) {
            assertWithMessage("title not defined for $className").that(titleIdentifier)
                .isNotEqualTo(0)
            // info can be null, so we do not assert on it, but we test if it resolves without raising
            // exception
            resolveStringOrArray("help_" + className + "_" + "_info")
        } else {
            for (variantName in helpVariants) {
                //if there is no generic title, variant specific ones are required
                if (titleIdentifier == 0) assertWithMessage(
                    "title not defined for $className, variant $variantName and no generic title exists"
                ).that(
                    getStringIdentifier("help_" + className + "_" + variantName + "_title")
                ).isNotEqualTo(0)
                // info can be null, so we do not assert on it, but we test if it resolves without raising
                // exception
                resolveStringOrArray("help_" + className + "_" + variantName + "_info")
                menuItemsIdentifier =
                    getArrayIdentifier(className + "_" + variantName + "_menuitems")
                if (menuItemsIdentifier != 0) {
                    testMenuItems(
                        className,
                        variantName,
                        resources.getStringArray(menuItemsIdentifier),
                        "menu"
                    )
                }
                menuItemsIdentifier =
                    getArrayIdentifier(className + "_" + variantName + "_cabitems")
                if (menuItemsIdentifier != 0) {
                    testMenuItems(
                        className,
                        variantName,
                        resources.getStringArray(menuItemsIdentifier),
                        "cab"
                    )
                }
            }
        }
    }

    private fun getHelpVariants(clazz: Class<out ProtectedFragmentActivity>): List<String> =
        when (clazz) {
            ExpenseEdit::class.java -> listOf(
                HELP_VARIANT_TRANSACTION, HELP_VARIANT_TRANSFER, HELP_VARIANT_SPLIT,
                HELP_VARIANT_TEMPLATE_CATEGORY, HELP_VARIANT_TEMPLATE_TRANSFER,
                HELP_VARIANT_TEMPLATE_SPLIT, HELP_VARIANT_SPLIT_PART_CATEGORY,
                HELP_VARIANT_SPLIT_PART_TRANSFER
            )

            ManageParties::class.java -> listOf(
                HELP_VARIANT_MERGE_MODE
            )

            ManageCategories::class.java, ManageTags::class.java -> listOf(
                HELP_VARIANT_MANGE, HELP_VARIANT_SELECT_FILTER, HELP_VARIANT_SELECT_MAPPING
            )

            ManageTemplates::class.java -> listOf(
                HELP_VARIANT_TEMPLATES,
                HELP_VARIANT_PLANS,
                HELP_VARIANT_PLANNER
            )

            else -> emptyList()
        }
}