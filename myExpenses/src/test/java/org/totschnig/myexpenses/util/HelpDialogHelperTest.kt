package org.totschnig.myexpenses.util

import android.app.Application
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
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
import org.totschnig.myexpenses.activity.HelpVariant
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
class HelpDialogHelperTest(private val activity: Class<out ProtectedFragmentActivity>) {
    private val context: Application
        get() = ApplicationProvider.getApplicationContext()
    private val resources: Resources
        get() = context.resources
    private lateinit var helper: HelpDialogHelper

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        //TODO complete with new activities
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
        helper = HelpDialogHelper(context)
        val pack: String = context.packageName
        var menuItemsIdentifier: Int
        val className = activity.simpleName
        Truth.assertThat(activity).isAssignableTo(ProtectedFragmentActivity::class.java)
        val titleIdentifier: Int =
            resources.getIdentifier("help_" + className + "_title", "string", pack)
        menuItemsIdentifier = resources.getIdentifier(className + "_menuitems", "array", pack)
        if (menuItemsIdentifier != 0) {
            testMenuItems(
                className,
                null,
                resources.getStringArray(menuItemsIdentifier),
                "menu"
            )
        }
        menuItemsIdentifier = resources.getIdentifier(className + "_cabitems", "array", pack)
        if (menuItemsIdentifier != 0) {
            testMenuItems(className, null, resources.getStringArray(menuItemsIdentifier), "cab")
        }
        menuItemsIdentifier = resources.getIdentifier(className + "_formfields", "array", pack)
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
            Truth.assertWithMessage("title not defined for $className").that(titleIdentifier)
                .isNotEqualTo(0)
        } else {
            for (variantName in helpVariants) {
                //if there is no generic title, variant specifc ones are required
                if (titleIdentifier == 0) Truth.assertWithMessage(
                    "title not defined for $className, variant $variantName and no generic title exists"
                ).that(
                    resources.getIdentifier(
                        "help_" + className + "_" + variantName + "_title",
                        "string",
                        pack
                    )
                ).isNotEqualTo(0)
                menuItemsIdentifier = resources.getIdentifier(
                    className + "_" + variantName + "_menuitems",
                    "array",
                    pack
                )
                if (menuItemsIdentifier != 0) {
                    testMenuItems(
                        className,
                        variantName,
                        resources.getStringArray(menuItemsIdentifier),
                        "menu"
                    )
                }
                menuItemsIdentifier = resources.getIdentifier(
                    className + "_" + variantName + "_cabitems",
                    "array",
                    pack
                )
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

    private fun getHelpVariants(clazz: Class<out ProtectedFragmentActivity>) = when (clazz) {
        ExpenseEdit::class.java -> ExpenseEdit.HelpVariant.values()
        ManageCategories::class.java, ManageParties::class.java, ManageTags::class.java -> HelpVariant.values()
        ManageTemplates::class.java -> ManageTemplates.HelpVariant.values()
        else -> null
    }?.map { it.toString() } ?: emptyList()

    private fun testMenuItems(
        activityName: String,
        variant: String?,
        menuItems: Array<String>,
        prefix: String
    ) {
        for (item in menuItems) {
            Truth.assertWithMessage(
                "title not found for %s-%s-%s-%s",
                activityName,
                variant,
                prefix,
                item
            ).that(helper.resolveTitle(item, prefix)).isNotEmpty()
            if (!resolveStringOrArray(prefix + "_" + activityName + "_" + variant + "_" + item + "_help_text")) {
                if (!resolveStringOrArray(prefix + "_" + activityName + "_" + item + "_help_text")) {
                    Truth.assertWithMessage(
                        "help text not found for %s-%s-%s-%s",
                        activityName,
                        variant,
                        prefix,
                        item
                    ).that(resolveStringOrArray(prefix + "_" + item + "_help_text")).isTrue()
                }
            }
        }
    }

    private fun resolveStringOrArray(resString: String): Boolean {
        return !helper.resolveStringOrArray(resString, false).isNullOrEmpty()
    }
}