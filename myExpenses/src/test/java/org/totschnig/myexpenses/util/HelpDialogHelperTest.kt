package org.totschnig.myexpenses.util

import android.app.Application
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.activity.AccountEdit
import org.totschnig.myexpenses.activity.BudgetActivity
import org.totschnig.myexpenses.activity.BudgetEdit
import org.totschnig.myexpenses.activity.CsvImportActivity
import org.totschnig.myexpenses.activity.DebtEdit
import org.totschnig.myexpenses.activity.DistributionActivity
import org.totschnig.myexpenses.activity.ExpenseEdit
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

@RunWith(RobolectricTestRunner::class)
class HelpDialogHelperTest {
    val context: Application
        get() = ApplicationProvider.getApplicationContext()
    private val resources: Resources
        get() = context.resources
    lateinit var helper: HelpDialogHelper


    @Test
    fun testHelpStringResExists() {
        helper = HelpDialogHelper(context)
        val pack: String = context.packageName
        var menuItemsIdentifier: Int
        //TODO complete with new activities
        val activities = arrayOf<Class<*>>(
            AccountEdit::class.java,
            BudgetActivity::class.java,
            BudgetEdit::class.java,
            CsvImportActivity::class.java,
            DebtEdit::class.java,
            DistributionActivity::class.java,
            ExpenseEdit::class.java,
            HistoryActivity::class.java,
            ManageBudgets::class.java,
            ManageCategories::class.java,
            ManageCurrencies::class.java,
            ManageMethods::class.java,
            ManageParties::class.java,
            ManageStaleImages::class.java,
            ManageSyncBackends::class.java,
            ManageTags::class.java,
            ManageTemplates::class.java,
            MethodEdit::class.java,
            MyExpenses::class.java,
            RoadmapVoteActivity::class.java
        )
        for (activity in activities) {
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
            try {
                val variants = Class.forName(activity.name + "$" + "HelpVariant") as Class<Enum<*>>
                for (variant in variants.enumConstants) {
                    val variantName = variant.name
                    //if there is no generic title, variant specifc ones are required
                    if (titleIdentifier == 0) Truth.assertWithMessage(
                        "title not defined for $className, variant $variantName and no generic title exists").that(
                        resources.getIdentifier(
                            "help_" + className + "_" + variantName + "_title",
                            "string",
                            pack
                        )).isNotEqualTo(0)
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
            } catch (e: ClassNotFoundException) {
                //title if there are no variants
                Truth.assertWithMessage("title not defined for $className").that(titleIdentifier).isNotEqualTo(0)
            }
        }
    }

    private fun testMenuItems(
        activityName: String,
        variant: String?,
        menuItems: Array<String>,
        prefix: String
    ) {
        for (item in menuItems) {
            Truth.assertWithMessage("title not found for %s-%s-%s-%s",
                activityName,
                variant,
                prefix,
                item).that(helper.resolveTitle(item, prefix)).isNotEmpty()
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