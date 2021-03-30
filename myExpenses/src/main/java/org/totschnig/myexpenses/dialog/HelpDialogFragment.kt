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
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Html.ImageGetter
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.HelpDialogActionRowBinding
import org.totschnig.myexpenses.databinding.HelpDialogBinding
import org.totschnig.myexpenses.util.distrib.DistributionHelper.isGithub
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.util.*

/**
 * A Dialog Fragment that displays help information. The content is constructed from resources
 * based on the activity and an optional variant passed in.
 *
 * @author Michael Totschnig
 */
class HelpDialogFragment : BaseDialogFragment(), ImageGetter {
    private var _binding: HelpDialogBinding? = null
    private val binding get() = _binding!!
    companion object {
        const val KEY_VARIANT = "variant"
        private const val KEY_CONTEXT = "context"
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
                "create_folder" to R.drawable.ic_menu_add,
                "up" to R.drawable.ic_arrow_upward,
                "categories_export" to R.drawable.ic_menu_download,
                "split_transaction" to R.drawable.ic_menu_split_transaction,
                "ungroup_split_transaction" to R.drawable.ic_menu_split,
                "move" to R.drawable.ic_menu_move,
                "sort" to R.drawable.ic_menu_sort,
                "sort_direction" to R.drawable.ic_menu_sort,
                "sort_up" to R.drawable.ic_arrow_upward,
                "sort_down" to R.drawable.ic_arrow_downward,
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
                "hidden_accounts" to R.drawable.design_ic_visibility_off,
                "hide" to R.drawable.design_ic_visibility_off,
                "close.reopen" to R.drawable.ic_lock,
                "remap" to null,
                "scan_mode" to R.drawable.ic_scan,
                "save_and_new" to R.drawable.ic_action_save_new,
                "link" to R.drawable.ic_hchain
        )

        @JvmStatic
        fun newInstance(context: String?, variant: String?): HelpDialogFragment {
            val dialogFragment = HelpDialogFragment()
            val args = Bundle()
            args.putString(KEY_CONTEXT, context)
            if (variant != null) {
                args.putString(KEY_VARIANT, variant)
            }
            dialogFragment.arguments = args
            return dialogFragment
        }
    }

    private var context: String? = null
    private var variant: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = activity
        val res = resources
        val title: String
        val args = arguments
        context = args!!.getString(KEY_CONTEXT)
        variant = args.getString(KEY_VARIANT)
        val builder = initBuilderWithBinding {
            HelpDialogBinding.inflate(materialLayoutInflater).also { _binding = it }
        }
        try {
            var resIdString = "help_" + context + "_info"
            var screenInfo = resolveStringOrArray(resIdString, true)
            if (variant != null) {
                resIdString = "help_" + context + "_" + variant + "_info"
                val variantInfo = resolveStringOrArray(resIdString, true)
                if (!TextUtils.isEmpty(variantInfo)) {
                    screenInfo = if (!TextUtils.isEmpty(screenInfo)) {
                        TextUtils.concat(screenInfo, HtmlCompat.fromHtml("<br>", FROM_HTML_MODE_LEGACY), variantInfo)
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
            val titleResId = if (variant != null) resolveString("help_" + context + "_" + variant + "_title") else 0
            title = if (titleResId == 0) {
                resolveStringOrThrowIf0("help_" + context + "_title")
            } else {
                getString(titleResId)
            }
        } catch (e: Resources.NotFoundException) {
            CrashHandler.report(e)
            return MaterialAlertDialogBuilder(ctx!!)
                    .setMessage("Error generating Help dialog")
                    .create()
        }
        /*    view.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        final ObjectAnimator animScrollToTop = ObjectAnimator.ofInt(view, "scrollY", 4000);
        animScrollToTop.setDuration(4000);
        animScrollToTop.start();
        return true;
      }
    });*/return builder.setTitle(title)
                .setIcon(R.drawable.ic_menu_help)
                .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                    if (activity == null) {
                        return@setPositiveButton
                    }
                    requireActivity().finish()
                }
                .create()
    }

    private fun showLongTapHint(componentName: String): Boolean {
        return componentName != "ManageTemplates_planner_cabitems"
    }

    private fun findComponentArray(type: String) = resolveArray(buildComponentName(type)).takeIf { it != 0 || variant == null }
            ?: resolveArray(context + "_" + type)

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
    private fun handleMenuItems(menuItems: ArrayList<String>, prefix: String, container: ViewGroup) {
        var resIdString: String
        var resId: Int?
        for (item in menuItems) {
            val rowBinding = HelpDialogActionRowBinding.inflate(materialLayoutInflater, container, false)
            var title = ""
            //this allows us to map an item like "date.time" to the concatenation of translations for date and for time
            for (resIdPart in item.split(".").toTypedArray()) {
                if (title != "") title += "/"
                title += resolveStringOrThrowIf0((if (prefix == "form") "" else "menu_") + resIdPart)
            }
            rowBinding.title.text = title
            if (prefix != "form") {
                if (iconMap.containsKey(item)) {
                    resId = iconMap[item]
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
            var helpText: CharSequence?
            helpText = resolveStringOrArray(prefix + "_" + context + "_" + variant + "_" + item + "_help_text", false)
            if (TextUtils.isEmpty(helpText)) {
                helpText = resolveStringOrArray(prefix + "_" + context + "_" + item + "_help_text", false)
                if (TextUtils.isEmpty(helpText)) {
                    resIdString = prefix + "_" + item + "_help_text"
                    helpText = resolveStringOrArray(resIdString, false)
                    if (TextUtils.isEmpty(helpText)) {
                        throw Resources.NotFoundException(resIdString)
                    }
                }
            }
            rowBinding.helpText.text = helpText
            container.addView(rowBinding.root)
        }
    }

    private fun resolveStringOrArray(resString: String, separateComponentsByLineFeeds: Boolean): CharSequence? {
        val resIdString = resString.replace('.', '_')
        val arrayId = resolveArray(resIdString)
        return if (arrayId == 0) {
            val stringId = resolveString(resIdString)
            if (stringId == 0) {
                null
            } else {
                HtmlCompat.fromHtml(getStringSafe(stringId), FROM_HTML_MODE_LEGACY, this, null)
            }
        } else {
            val linefeed: CharSequence = HtmlCompat.fromHtml("<br>", FROM_HTML_MODE_LEGACY)

            val components = resources.getStringArray(arrayId)
                    .filter { component -> !shouldSkip(component) }
                    .map { component -> handle(component) }
            val resolvedComponents = ArrayList<CharSequence>()
            for (i in components.indices) {
                resolvedComponents.add(HtmlCompat.fromHtml(components[i], FROM_HTML_MODE_LEGACY, this, null))
                if (i < components.size - 1) {
                    resolvedComponents.add(if (separateComponentsByLineFeeds) linefeed else " ")
                }
            }
            TextUtils.concat(*resolvedComponents.toTypedArray())
        }
    }

    private fun handle(component: String): String {
        return if (component.startsWith("popup")) {
            resolveName(component + "_intro") + " " + resources.getStringArray(resolveArray(component + "_items")).joinToString(" ") {
                "<b>${resolveName(it)}</b>: ${resolveName(component + "_" + it)}"
            }
        } else {
            resolveName(component)
        }
    }

    private fun resolveName(name: String) = getStringSafe(resolveString(name))

    private fun shouldSkip(component: String): Boolean {
        when (component) {
            "help_ManageSyncBackends_drive" -> return isGithub
        }
        return false
    }

    @StringRes
    private fun resolveString(resIdString: String): Int {
        return resolve(resIdString, "string")
    }

    /**
     * @throws Resources.NotFoundException if there is no resource for the given String. On the contrary, if the
     * String does exist in an alternate locale, but not in the default one,
     * the resulting exception is caught and empty String is returned.
     */
    @Throws(Resources.NotFoundException::class)
    private fun resolveStringOrThrowIf0(resIdString: String): String {
        val resId = resolveString(resIdString)
        if (resId == 0) {
            throw Resources.NotFoundException(resIdString)
        }
        return getStringSafe(resId)
    }

    private fun getStringSafe(resId: Int): String {
        return try {
            resources.getString(resId)
        } catch (e: Resources.NotFoundException) { //if resource does exist in an alternate locale, but not in the default one
            ""
        }
    }

    @ArrayRes
    private fun resolveArray(resIdString: String): Int {
        return resolve(resIdString, "array")
    }

    private fun resolve(resIdString: String, defType: String): Int {
        return resolve(resources, resIdString, defType, requireActivity().packageName)
    }

    private fun resolveSystem(resIdString: String, @Suppress("SameParameterValue") defType: String): Int {
        return resolve(Resources.getSystem(), resIdString, defType, "android")
    }

    private fun resolve(resources: Resources, resIdString: String, defType: String, packageName: String): Int {
        return resources.getIdentifier(resIdString, defType, packageName)
    }

    override fun onCancel(dialog: DialogInterface) {
        requireActivity().finish()
    }

    override fun getDrawable(name: String): Drawable? {
        val theme = requireActivity().theme
        return try {
            //Keeping the legacy attribute reference in order to not have to update all translations, where
            //it appears
            val resId = if (name.startsWith("?")) {
                with(name.substring(1)) {
                    when (this) {
                        "calcIcon" -> R.drawable.ic_action_equal
                        else -> {
                            val value = TypedValue()
                            theme.resolveAttribute(resolve(this, "attr"), value, true)
                            value.resourceId
                        }
                    }
                }
            } else {
                if (name.startsWith("android:")) {
                    resolveSystem(name.substring(8), "drawable")
                } else {
                    resolve(name, "drawable")
                }
            }
            return ResourcesCompat.getDrawable(resources, resId, theme)?.apply {
                setBounds(0, 0, intrinsicWidth / 2, intrinsicHeight / 2)
            }
        } catch (e: Resources.NotFoundException) {
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}