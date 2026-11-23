/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.totschnig.myexpenses.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Resources
import android.os.Bundle
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.HelpDialogActionRowBinding
import org.totschnig.myexpenses.databinding.HelpDialogBinding
import org.totschnig.myexpenses.util.HelpDialogHelper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import timber.log.Timber

/**
 * A Dialog Fragment that displays help information. The content is constructed from resources
 * based on the activity and an optional variant passed in.
 *
 * @author Michael Totschnig
 */
class HelpDialogFragment : DialogViewBinding<HelpDialogBinding>() {

    lateinit var helper: HelpDialogHelper

    companion object {
        const val KEY_VARIANT = "variant"
        const val KEY_CONTEXT = "context"
        const val KEY_TITLE = "title"
        const val KEY_EXTRA = "extra"
        private const val KEY_SELECTED_VARIANT = "selected_variant"
        private val iconMap: Map<String, Int?> = mapOf(
            "edit" to R.drawable.ic_menu_edit,
            "back" to R.drawable.ic_menu_back,
            "balance" to R.drawable.ic_action_balance,
            "cancel_plan_instance" to R.drawable.ic_menu_close_clear_cancel,
            "categories_setup_default" to R.drawable.ic_menu_add_list,
            "clone_transaction" to R.drawable.ic_menu_copy,
            "create_instance_edit" to R.drawable.ic_action_apply_edit,
            "create_instance_save" to R.drawable.ic_action_apply_save,
            "create_account" to R.drawable.account_balance,
            "create_split" to R.drawable.ic_menu_split,
            "create_sub_cat" to R.drawable.ic_menu_add,
            "delete" to R.drawable.ic_menu_delete,
            "distribution" to R.drawable.ic_menu_chart,
            "edit_plan_instance" to R.drawable.ic_menu_edit,
            "forward" to R.drawable.ic_menu_forward,
            "invert_transfer" to R.drawable.ic_menu_move,
            "manage_plans" to R.drawable.ic_menu_template,
            "templates" to R.drawable.ic_menu_template,
            "reset" to R.drawable.ic_menu_download,
            "reset_plan_instance" to R.drawable.ic_menu_revert,
            "save" to R.drawable.ic_menu_done,
            "search" to R.drawable.ic_menu_search,
            "select" to R.drawable.ic_menu_done,
            "print" to R.drawable.ic_menu_print,
            "create_template_from_transaction" to R.drawable.ic_action_template_add,
            "categories_export" to R.drawable.ic_menu_download,
            "split_transaction" to R.drawable.ic_menu_split_transaction,
            "ungroup_split_transaction" to R.drawable.ic_menu_split,
            "move" to R.drawable.ic_menu_move,
            "sort" to R.drawable.ic_menu_sort,
            "sort_direction" to R.drawable.ic_menu_sort,
            "grouping" to R.drawable.ic_action_group,
            "create_sync_backend" to R.drawable.ic_menu_add,
            "sync" to R.drawable.ic_sync,
            "sync_now" to null,
            "remove" to null,
            "sync_download" to null,
            "sync_link" to null,
            "sync_unlink" to null,
            "vote" to null,
            "refresh" to R.drawable.ic_sync,
            "result" to R.drawable.ic_web,
            "clear" to R.drawable.ic_menu_close_clear_cancel,
            "learn_more" to null,
            "set_weight" to null,
            "original_amount" to null,
            "equivalent_amount" to null,
            "color" to R.drawable.ic_color,
            "history" to R.drawable.ic_history,
            "budget" to R.drawable.ic_budget,
            "manage_categories" to null,
            "show_transactions" to null,
            "back.forward" to null,
            "hide" to R.drawable.ic_hide,
            "close.reopen" to R.drawable.ic_lock,
            "remap" to R.drawable.redo,
            "scan_mode" to R.drawable.ic_scan,
            "save_and_new" to R.drawable.ic_action_save_new,
            "link" to R.drawable.ic_link,
            "merge" to R.drawable.ic_menu_split_transaction,
            "parties.debts" to R.drawable.ic_group,
            "debts" to R.drawable.balance_scale,
            "rollover" to null,
            "type_filter" to R.drawable.ic_filter,
            "archive" to R.drawable.ic_archive,
            "budget_synchronization" to R.drawable.ic_sync,
            "download" to R.drawable.ic_menu_download,
            "add" to R.drawable.ic_menu_add,
            "account_types" to R.drawable.category,
            "account_flags" to R.drawable.ic_flag,
            "flag" to R.drawable.ic_flag,
            "balance_sheet" to R.drawable.ic_table_chart,
            "create_portfolio" to R.drawable.ic_history,
            "expense" to R.drawable.ic_expense,
            "income" to R.drawable.ic_menu_add,
            "transfer" to R.drawable.ic_menu_forward,
            "split" to R.drawable.ic_menu_split,
            "scan" to R.drawable.ic_scan,
            "trade_buy" to R.drawable.ic_debt_up,
            "trade_sell" to R.drawable.ic_debt_down,
            "trade_deposit" to R.drawable.ic_menu_add,
            "trade_withdraw" to R.drawable.ic_expense,
            "trade_transfer" to R.drawable.ic_menu_forward
        )

        @JvmStatic
        fun newInstance(extras: Bundle?) =
            HelpDialogFragment().apply { arguments = extras }
    }

    private var context: String? = null
    private var variant: String? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SELECTED_VARIANT, variant)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = requireActivity()
        val args = requireArguments()
        helper = HelpDialogHelper(ctx, args.getCharSequence(KEY_EXTRA))
        context = args.getString(KEY_CONTEXT)
        val savedSelectedVariant = savedInstanceState?.getString(KEY_SELECTED_VARIANT)
        val rawVariant = args.getString(KEY_VARIANT)
        val variantsList = if (!rawVariant.isNullOrEmpty()) rawVariant.split(",") else emptyList()
        variant = savedSelectedVariant?.takeIf { it in variantsList } ?: variantsList.firstOrNull()

        if (context == null) return onError("context extra missing")
        Timber.d("Help for $context / $rawVariant")
        val builder = initBuilder {
            HelpDialogBinding.inflate(it)
        }
        try {
            setupTabLayout(variantsList, variant)
            populateContent(variant)
            val title = resolveDialogTitle(args, variantsList)

            return builder.setTitle(title)
                .setIcon(R.drawable.ic_menu_help)
                .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                    if (activity == null) {
                        return@setPositiveButton
                    }
                    requireActivity().finish()
                }
                .create()
        } catch (e: Resources.NotFoundException) {
            CrashHandler.report(e)
            return onError("Error generating Help dialog")
        }
    }

    private fun setupTabLayout(variantsList: List<String>, currentVariant: String?) {
        if (variantsList.size <= 1) {
            binding.tabLayout.visibility = View.GONE
            return
        }
        binding.tabLayout.visibility = View.VISIBLE
        binding.tabLayout.removeAllTabs()
        var tabToSelect: TabLayout.Tab? = null
        for (v in variantsList) {
            val titleResId = helper.resolveString("help_" + context + "_" + v + "_title")
            val tabTitle = if (titleResId != 0) getString(titleResId) else v
            val tab = binding.tabLayout.newTab().setText(tabTitle).setTag(v)
            binding.tabLayout.addTab(tab)
            if (v == currentVariant) {
                tabToSelect = tab
            }
        }
        binding.tabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val selectedVariant = tab.tag as? String ?: return
                variant = selectedVariant
                populateContent(selectedVariant)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        tabToSelect?.select()
    }

    private fun resolveDialogTitle(args: Bundle, variantsList: List<String>): CharSequence =
        args.getString(KEY_TITLE) ?: if (variantsList.size > 1) {
            getString(R.string.menu_help)
        } else {
            val titleResId = if (variant != null) {
                helper.resolveString("help_" + context + "_" + variant + "_title")
            } else {
                0
            }
            if (titleResId == 0) {
                helper.getStringOrThrowIf0("help_" + context + "_title")
            } else {
                getString(titleResId)
            }
        }

    private fun populateContent(currentVariant: String?) {
        populateScreenInfo(currentVariant)
        populateSection(
            currentVariant,
            "formfields",
            "form",
            binding.formFieldsHeading,
            binding.formFieldsContainer
        )
        populateSection(
            currentVariant,
            "menuitems",
            "menu",
            binding.menuCommandsHeading,
            binding.menuCommandsContainer
        )
        populateSection(
            currentVariant,
            "fabactions",
            "fab",
            binding.fabActionsHeading,
            binding.fabActionsContainer
        )
        populateSection(
            currentVariant,
            "cabitems",
            "cab",
            binding.cabCommandsHeading,
            binding.cabCommandsContainer,
            binding.cabCommandsHelp
        )
        binding.aboutscrollview.scrollTo(0, 0)
    }

    private fun populateScreenInfo(currentVariant: String?) {
        var resIdString = "help_" + context + "_info"
        var screenInfo = helper.resolveStringOrArray(resIdString, true)
        if (currentVariant != null) {
            resIdString = "help_" + context + "_" + currentVariant + "_info"
            val variantInfo = helper.resolveStringOrArray(resIdString, true)
            if (!TextUtils.isEmpty(variantInfo)) {
                screenInfo = if (!TextUtils.isEmpty(screenInfo)) {
                    TextUtils.concat(
                        screenInfo,
                        "\n",
                        variantInfo
                    )
                } else {
                    variantInfo
                }
            }
        }
        if (TextUtils.isEmpty(screenInfo)) {
            binding.screenInfo.visibility = View.GONE
        } else {
            binding.screenInfo.visibility = View.VISIBLE
            binding.screenInfo.text = screenInfo
            binding.screenInfo.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun populateSection(
        currentVariant: String?,
        arrayType: String,
        prefix: String,
        heading: View,
        container: ViewGroup,
        helpTextHint: View? = null
    ) {
        val resId = findComponentArray(currentVariant, arrayType)
        val items = if (resId != 0) resources.getStringArray(resId) else emptyArray()
        container.removeAllViews()
        if (items.isEmpty()) {
            heading.visibility = View.GONE
            helpTextHint?.visibility = View.GONE
        } else {
            heading.visibility = View.VISIBLE
            if (helpTextHint != null) {
                helpTextHint.visibility =
                    if (showLongTapHint(buildComponentName(currentVariant, arrayType))) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
            }
            handleMenuItems(currentVariant, ArrayList(items.toList()), prefix, container)
        }
    }

    private fun onError(message: String) = MaterialAlertDialogBuilder(requireActivity())
        .setMessage(message)
        .create()

    private fun showLongTapHint(componentName: String) =
        componentName !in arrayOf(
            "ManageTemplates_plans_cabitems",
            "ManageTemplates_planner_cabitems",
            "ManageParties_manage_cabitems",
            "ManageCategories_manage_cabitems",
            "ManageCategories_select_filter_cabitems",
            "RoadmapVoteActivity_cabitems",
            "PriceHistory_cabitems",
            "MyExpensesV2_accounts_cabitems",
            "MyExpensesV2_transactions_cabitems"
        )

    private fun findComponentArray(currentVariant: String?, type: String) =
        helper.resolveArray(buildComponentName(currentVariant, type))
            .takeIf { it != 0 || currentVariant == null }
            ?: helper.resolveArray(context + "_" + type)

    private fun buildComponentName(currentVariant: String?, type: String): String =
        if (currentVariant !=
            null
        ) {
            context + "_" + currentVariant + "_" + type
        } else {
            context + "_" + type
        }

    /**
     * @param menuItems list of menuItems to be displayed
     * @param prefix    "form", "menu" or "cab"
     * @param container items will be added to this container
     * @throws Resources.NotFoundException
     */
    @Throws(Resources.NotFoundException::class)
    private fun handleMenuItems(
        currentVariant: String?,
        menuItems: ArrayList<String>,
        prefix: String,
        container: ViewGroup
    ) {
        for (item in menuItems) {
            val rowBinding =
                HelpDialogActionRowBinding.inflate(materialLayoutInflater, container, false)
            val title = helper.resolveTitle(item, prefix)
            rowBinding.title.text = title
            if (prefix != "form") {
                if (iconMap.containsKey(item)) {
                    val resId = iconMap[item]
                    if (resId != null) {
                        with(rowBinding.listImage) {
                            visibility = View.VISIBLE
                            setImageResource(resId)
                            contentDescription = title
                        }
                    }
                } else {
                    rowBinding.listCheckbox.visibility = View.VISIBLE
                    // menu entries without entries in the map are checkable
                }
            }

            // we look for a help text specific to the variant first, then to the activity
            // and last a generic one
            // We look for an array first, which allows us to compose messages of parts
            val helpText = currentVariant?.let {
                helper.resolveStringOrArray(
                    prefix + "_" + context + "_" + it + "_" + item + "_help_text",
                    false
                )
            }?.takeIf { it.isNotEmpty() } ?: helper.resolveStringOrArray(
                prefix + "_" + context + "_" + item + "_help_text",
                false
            )?.takeIf { it.isNotEmpty() } ?: kotlin.run {
                val resIdString = prefix + "_" + item + "_help_text"
                helper.resolveStringOrArray(resIdString, false)
                    ?.takeIf { it.isNotEmpty() }
                    ?: throw Resources.NotFoundException(resIdString)
            }
            rowBinding.helpText.movementMethod = LinkMovementMethod.getInstance()
            rowBinding.helpText.text = helpText
            container.addView(rowBinding.root)
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        requireActivity().finish()
    }
}
