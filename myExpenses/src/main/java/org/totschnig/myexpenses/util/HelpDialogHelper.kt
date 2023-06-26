package org.totschnig.myexpenses.util

import android.content.Context
import android.content.res.Resources
import android.text.Html.ImageGetter
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.util.TypedValue
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.bold
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.distrib.DistributionHelper

class HelpDialogHelper(val context: Context) : ImageGetter {
    val resources: Resources = context.resources

    @Throws(Resources.NotFoundException::class)
    fun getStringArray(@ArrayRes id: Int): Array<String> = resources.getStringArray(id)

    @Throws(Resources.NotFoundException::class)
    fun getString(@StringRes id: Int, vararg formatArgs: Any?): String =
        resources.getString(id, formatArgs)

    fun resolveTitle(
        item: String,
        prefix: String
    ): String =
        item.split(".").joinToString("/") {
            getStringOrThrowIf0((if (prefix == "form") "" else "menu_") + it)
        }

    fun resolveStringOrArray(
        resString: String,
        separateComponentsByLineFeeds: Boolean
    ): CharSequence? {
        fun toTitle(resId: Int) = SpannableStringBuilder()
            .append(" ")
            .bold { append(getString(resId)) }
            .append(": ")
        return when (resString) {
            "menu_BudgetActivity_rollover_help_text" -> {
                TextUtils.concat(*buildList {
                    add(getString(R.string.menu_BudgetActivity_rollover_help_text))
                    add(toTitle(R.string.menu_aggregates))
                    add(getString(R.string.menu_BudgetActivity_rollover_total))
                    add(toTitle(R.string.pref_manage_categories_title))
                    add(getString(R.string.menu_BudgetActivity_rollover_categories))
                    add(toTitle(R.string.menu_edit))
                    add(getString(R.string.menu_BudgetActivity_rollover_edit))
                }.toTypedArray())
            }
            else -> {
                val resIdString = resString.replace('.', '_')
                val arrayId = resolveArray(resIdString)
                if (arrayId == 0) {
                    val stringId = resolveString(resIdString)
                    if (stringId == 0) {
                        null
                    } else {
                        HtmlCompat.fromHtml(
                            getString(stringId),
                            HtmlCompat.FROM_HTML_MODE_LEGACY, this, null
                        )
                    }
                } else {

                    val components = resources.getStringArray(arrayId)
                        .filter { component -> !shouldSkip(component) }
                        .map { component -> handle(component) }
                    TextUtils.concat(*buildList {
                        for (i in components.indices) {
                            this.add(
                                HtmlCompat.fromHtml(
                                    components[i],
                                    HtmlCompat.FROM_HTML_MODE_LEGACY,
                                    this@HelpDialogHelper,
                                    null
                                )
                            )
                            if (i < components.size - 1) {
                                this.add(if (separateComponentsByLineFeeds) "\n" else " ")
                            }
                        }
                    }.toTypedArray())
                }
            }
        }
    }

    private fun handle(component: String) = if (component.startsWith("popup")) {
        getStringOrThrowIf0(component + "_intro") + " " +
                getStringArray(
                    resolveArray(
                        component + "_items"
                    )
                ).joinToString(" ") {
                    "<b>${getStringOrThrowIf0(it)}</b>: ${getStringOrThrowIf0(component + "_" + it)}"
                }
    } else {
        (getStringOrNull(component + "_title")
            ?.let { "<b>$it</b> " } ?: "") +
                getStringOrThrowIf0(component)
    }

    private fun shouldSkip(component: String) = when (component) {
        "help_ManageSyncBackends_drive" -> DistributionHelper.isGithub
        else -> false
    }

    @StringRes
    fun resolveString(resIdString: String) = resolve(resIdString, "string")

    /**
     * @throws Resources.NotFoundException if there is no resource for the given String. On the contrary, if the
     * String does exist in an alternate locale, but not in the default one,
     * the resulting exception is caught and empty String is returned.
     */
    @Throws(Resources.NotFoundException::class)
    fun getStringOrThrowIf0(resIdString: String) = when (resIdString) {
        "help_ManageTemplates_plans_info" -> arrayOf<CharSequence>(
            getString(R.string.help_ManageTemplates_plans_info_header),
            "<br><img src=\"ic_stat_open\"> ",
            getString(R.string.help_ManageTemplates_plans_info_open),
            "<br><img src=\"ic_stat_applied\"> ",
            getString(R.string.help_ManageTemplates_plans_info_applied),
            "<br><img src=\"ic_stat_cancelled\"> ",
            getString(R.string.help_ManageTemplates_plans_info_cancelled),
            "<br><img src=\"ic_warning\"> ",
            getString(R.string.orphaned_transaction_info)
        ).joinToString("")
        "menu_categories_export" -> {
            getString(R.string.export_to_format, "QIF")
        }
        else -> {
            getStringOrNull(resIdString) ?: throw Resources.NotFoundException(resIdString)
        }
    }

    fun getStringOrNull(resIdString: String) = resolveString(resIdString)
        .takeIf { it != 0 }
        ?.let { getString(it) }

    @ArrayRes
    fun resolveArray(resIdString: String) = resolve(resIdString, "array")

    private fun resolve(resIdString: String, defType: String) =
        resolve(resources, resIdString, defType, context.packageName)

    private fun resolveSystem(
        resIdString: String,
        @Suppress("SameParameterValue") defType: String
    ) = resolve(Resources.getSystem(), resIdString, defType, "android")

    private fun resolve(
        resources: Resources,
        resIdString: String,
        defType: String,
        packageName: String
    ) = resources.getIdentifier(resIdString, defType, packageName)

    override fun getDrawable(name: String) = try {
        //Keeping the legacy attribute reference in order to not have to update all translations, where
        //it appears
        val resId = if (name.startsWith("?")) {
            with(name.substring(1)) {
                when (this) {
                    "calcIcon" -> R.drawable.ic_action_equal
                    else -> {
                        val value = TypedValue()
                        context.theme.resolveAttribute(resolve(this, "attr"), value, true)
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
        val dimensionPixelSize =
            resources.getDimensionPixelSize(R.dimen.help_text_inline_icon_size)
        ResourcesCompat.getDrawable(resources, resId, context.theme)?.apply {
            setBounds(0, 0, dimensionPixelSize, dimensionPixelSize)
        }
    } catch (e: Resources.NotFoundException) {
        null
    }
}