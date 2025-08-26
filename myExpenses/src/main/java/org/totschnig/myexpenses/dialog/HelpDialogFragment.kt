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
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.HelpDialogActionRowBinding
import org.totschnig.myexpenses.databinding.HelpDialogBinding
import org.totschnig.myexpenses.util.HelpDialogHelper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler

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
        private val iconMap: Map<String, Int?> = mapOf(
            "edit" to R.drawable.ic_menu_edit,
            "back" to R.drawable.ic_menu_back,
            "balance" to R.drawable.ic_action_balance,
            "cancel_plan_instance" to R.drawable.ic_menu_close_clear_cancel,
            "categories_setup_default" to R.drawable.ic_menu_add_list,
            "clone_transaction" to R.drawable.ic_menu_copy,
            "create_instance_edit" to R.drawable.ic_action_apply_edit,
            "create_instance_save" to R.drawable.ic_action_apply_save,
            "create_account" to R.drawable.ic_menu_add,
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
            "balance_sheet" to R.drawable.ic_table_chart
        )

        @JvmStatic
        fun newInstance(extras: Bundle?) =
            HelpDialogFragment().apply { arguments = extras }
    }

    private var context: String? = null
    private var variant: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = requireActivity()
        val res = resources
        val args = requireArguments()
        helper = HelpDialogHelper(ctx, args.getCharSequence(KEY_EXTRA))
        context = args.getString(KEY_CONTEXT)
        variant = args.getString(KEY_VARIANT)
        if (context == null) return onError("context extra missing")
        val builder = initBuilder {
            HelpDialogBinding.inflate(it)
        }
        try {
            var resIdString = "help_" + context + "_info"
            var screenInfo = helper.resolveStringOrArray(resIdString, true)
            if (variant != null) {
                resIdString = "help_" + context + "_" + variant + "_info"
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
                binding.screenInfo.text = screenInfo
                binding.screenInfo.movementMethod = LinkMovementMethod.getInstance()
            }

            // Form entries
            val formResId = findComponentArray("formfields")
            val menuItems = ArrayList<String>()
            if (formResId != 0) {
                menuItems.addAll(listOf(*res.getStringArray(formResId)))
            }
            if (menuItems.isEmpty()) {
                binding.formFieldsHeading.visibility = View.GONE
            } else {
                handleMenuItems(menuItems, "form", binding.formFieldsContainer)
            }

            // Menu items
            val menuResId = findComponentArray("menuitems")
            menuItems.clear()
            if (menuResId != 0) menuItems.addAll(listOf(*res.getStringArray(menuResId)))
            if (menuItems.isEmpty()) {
                binding.menuCommandsHeading.visibility = View.GONE
            } else {
                handleMenuItems(menuItems, "menu", binding.menuCommandsContainer)
            }

            // Contextual action bar
            val cabResId = findComponentArray("cabitems")
            menuItems.clear()
            if (cabResId != 0) menuItems.addAll(listOf(*res.getStringArray(cabResId)))
            if (menuItems.isEmpty()) {
                binding.cabCommandsHeading.visibility = View.GONE
            } else {
                handleMenuItems(menuItems, "cab", binding.cabCommandsContainer)
            }
            if (menuItems.isEmpty() || !showLongTapHint(buildComponentName("cabitems"))) {
                binding.cabCommandsHelp.visibility = View.GONE
            }
            val titleResId =
                if (variant != null) helper.resolveString("help_" + context + "_" + variant + "_title") else 0
            val title = args.getString(KEY_TITLE) ?: if (titleResId == 0) {
                helper.getStringOrThrowIf0("help_" + context + "_title")
            } else {
                getString(titleResId)
            }
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

    private fun onError(message: String) = MaterialAlertDialogBuilder(requireActivity())
        .setMessage(message)
        .create()


    private fun showLongTapHint(componentName: String) =
        !arrayOf(
            "ManageTemplates_plans_cabitems",
            "ManageTemplates_planner_cabitems",
            "ManageParties_manage_cabitems",
            "ManageCategories_manage_cabitems",
            "ManageCategories_select_filter_cabitems",
            "MyExpenses_cabitems",
            "RoadmapVoteActivity_cabitems",
            "PriceHistory_cabitems"
        )
            .contains(componentName)

    private fun findComponentArray(type: String) =
        helper.resolveArray(buildComponentName(type)).takeIf { it != 0 || variant == null }
            ?: helper.resolveArray(context + "_" + type)

    private fun buildComponentName(type: String): String {
        return if (variant != null) context + "_" + variant + "_" + type else context + "_" + type
    }

    /**
     * @param menuItems list of menuItems to be displayed
     * @param prefix    "form", "menu" or "cab"
     * @param container items will be added to this container
     * @throws Resources.NotFoundException
     */
    @Throws(Resources.NotFoundException::class)
    private fun handleMenuItems(
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
                    //menu entries without entries in the map are checkable
                }
            }

            //we look for a help text specific to the variant first, then to the activity
            //and last a generic one
            //We look for an array first, which allows us to compose messages of parts
            val helpText = variant?.let {
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