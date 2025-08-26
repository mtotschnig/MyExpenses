package org.totschnig.myexpenses.util

import android.annotation.SuppressLint
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
import androidx.core.text.parseAsHtml
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.PrintLayoutConfiguration.Companion.screenTitle
import org.totschnig.myexpenses.retrofit.ExchangeRateApi
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.myexpenses.viewmodel.data.IIconInfo

class HelpDialogHelper(val context: Context, val extra: CharSequence? = null) : ImageGetter {
    val resources: Resources = context.resources

    @Throws(Resources.NotFoundException::class)
    fun getStringArray(@ArrayRes id: Int): Array<String> = resources.getStringArray(id)

    @Throws(Resources.NotFoundException::class)
    fun getString(@StringRes id: Int, vararg formatArgs: Any?): String =
        resources.getString(id, *formatArgs)

    fun resolveTitle(
        item: String,
        prefix: String,
    ): String =
        item.split(".").joinToString("/") {
            getStringOrThrowIf0((if (prefix == "form") "" else "menu_") + it)
        }

    fun resolveStringOrArray(
        resString: String,
        separateComponentsByLineFeeds: Boolean,
    ): CharSequence? {
        fun toTitle(resId: Int) = SpannableStringBuilder()
            .append(" ")
            .bold { append(getString(resId)) }
            .append(": ")
        return when (resString) {
            "menu_BudgetActivity_rollover_help_text" -> TextUtils.concat(*buildList {
                add(getString(R.string.menu_BudgetActivity_rollover_help_text))
                add(toTitle(R.string.menu_aggregates))
                add(getString(R.string.menu_BudgetActivity_rollover_total))
                add(toTitle(R.string.pref_manage_categories_title))
                add(getString(R.string.menu_BudgetActivity_rollover_categories))
                add(toTitle(R.string.menu_edit))
                add(getString(R.string.menu_BudgetActivity_rollover_edit))
            }.toTypedArray())

            "help_ManageStaleImages_info" -> getString(
                R.string.help_ManageStaleImages_info,
                "Documents/MyExpenses.Attachments.Archive"
            )

            "help_PriceHistory_info" -> TextUtils.concat(
                getString(R.string.help_PriceHistory_info),
                " ",
                SpannableStringBuilder().apply {
                    ExchangeRateApi.values.forEach {
                        bold { append(it.name.first()) }
                        append(" ")
                        append(it.name)
                        append(", ")
                    }
                },
                "<img src=icon:user> ${getString(R.string.help_user_exchange_rate)}, <img src=ic_calculate> ${
                    getString(
                        R.string.help_calculated_exchange_rate
                    )
                }".parseAsHtml(HtmlCompat.FROM_HTML_MODE_LEGACY, this@HelpDialogHelper)
            )

            "menu_NavigationDrawer_show_equivalent_value_help_text" -> TextUtils.concat(
                getString(R.string.menu_NavigationDrawer_show_equivalent_value_help_text_intro),
                "\n• ",
                resources.getText(R.string.menu_NavigationDrawer_show_equivalent_value_help_text_dynamic_1),
                "\n• ",
                resources.getText(R.string.menu_NavigationDrawer_show_equivalent_value_help_text_dynamic_2),
                "\n• ",
                resources.getText(R.string.menu_NavigationDrawer_show_equivalent_value_help_text_dynamic_3)
            )

            else -> {
                val resIdString = resString.replace('.', '_')
                val arrayId = resolveArray(resIdString)
                if (arrayId == 0) {
                    val stringId = resolveString(resIdString)
                    if (stringId == 0) {
                        null
                    } else {
                        if (isV2(resIdString)) resources.getText(stringId) else
                            getString(stringId).parseAsHtml(HtmlCompat.FROM_HTML_MODE_LEGACY, this)
                    }
                } else {

                    val components = resources.getStringArray(arrayId)
                        .filter { component -> !shouldSkip(component) }
                        .map { component -> component to handle(component) }
                    TextUtils.concat(*buildList {
                        for (i in components.indices) {
                            val (component, text) = components[i]
                            add(
                                if (isV2(component)) text else
                                    text.toString().parseAsHtml(
                                        HtmlCompat.FROM_HTML_MODE_LEGACY,
                                        this@HelpDialogHelper
                                    )
                            )
                            if (i < components.size - 1) {
                                add(if (separateComponentsByLineFeeds) "\n" else " ")
                            }
                        }
                    }.toTypedArray())
                }
            }
        }
    }

    private fun isV2(resIdString: String) = resIdString.contains("v2")


    private fun handle(component: String): CharSequence = if (component.startsWith("popup")) {
        TextUtils.concat(
            getStringOrThrowIf0(component + "_intro"),
            " ",
            getStringArray(
                resolveArray(
                    component + "_items"
                )
            ).joinToString(" ") {
                "<b>${getStringOrThrowIf0(it)}</b>: ${getStringOrThrowIf0(component + "_" + it)}"
            }
        )
    } else {
        TextUtils.concat(
            (getStringOrNull(component + "_title")
                ?.let { "<b>$it</b> " } ?: ""),
            getStringOrThrowIf0(component)
        )
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
    fun getStringOrThrowIf0(resIdString: String): CharSequence {
        fun toBold(resId: Int) = "<b>${getString(resId)}</b>"
        return when (resIdString) {
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

            "menu_categories_export" -> getString(R.string.export_to_format, "QIF")
            "help_ManageCategories_category_types" -> buildString {
                append(toBold(R.string.expense))
                append("/")
                append(toBold(R.string.income))
                append(": <a href='https://faq.myexpenses.mobi/category-types'>FAQ</a>")
            }

            "dynamic_exchange_rate_help_text_3" -> getString(
                R.string.dynamic_exchange_rate_help_text_3,
                context.localizedQuote(getString(R.string.enable_automatic_daily_exchange_rate_download))
            )

            "help_WebUI_info_1" -> if (extra == null || extra.startsWith("http"))
                getString(R.string.help_WebUI_info_1) + (extra?.let { "($it)" } ?: "") else ""

            "help_PrintLayoutConfiguration_title" -> context.screenTitle

            else ->
                getStringOrNull(resIdString) ?: throw Resources.NotFoundException(resIdString)
        }
    }

    fun getStringOrNull(resIdString: String): CharSequence? = resolveString(resIdString)
        .takeIf { it != 0 }
        ?.let {
            if (isV2(resIdString)) resources.getText(it) else getString(it)
        }

    @ArrayRes
    fun resolveArray(resIdString: String) = resolve(resIdString, "array")

    private fun resolve(resIdString: String, defType: String) =
        resolve(resources, resIdString, defType, context.packageName)

    private fun resolveSystem(
        resIdString: String,
        @Suppress("SameParameterValue") defType: String,
    ) = resolve(Resources.getSystem(), resIdString, defType, "android")

    @SuppressLint("DiscouragedApi")
    private fun resolve(
        resources: Resources,
        resIdString: String,
        defType: String,
        packageName: String,
    ) = resources.getIdentifier(resIdString, defType, packageName)

    override fun getDrawable(name: String) = (if (name.startsWith("icon:")) {
        IIconInfo.resolveIcon(name.substringAfter(':'))?.asDrawable(context)
    } else try {
        //Keeping the legacy attribute reference in order to not have to update all translations
        val resId = if (name.startsWith("?")) {
            with(name.substring(1)) {
                when (this) {
                    "calcIcon" -> R.drawable.ic_calculate
                    else -> {
                        val value = TypedValue()
                        context.theme.resolveAttribute(resolve(this, "attr"), value, true)
                        value.resourceId
                    }
                }
            }
        } else if (name.startsWith("android:")) {
            resolveSystem(name.substringAfter(':'), "drawable")
        } else {
            when (name) {
                //Keeping the legacy drawable name
                "ic_hchain" -> R.drawable.ic_link
                "ic_hchain_broken" -> R.drawable.ic_link_off
                else -> resolve(name, "drawable")
            }
        }
        ResourcesCompat.getDrawable(resources, resId, context.theme)
    } catch (_: Resources.NotFoundException) {
        null
    })?.apply {
        val dimensionPixelSize =
            resources.getDimensionPixelSize(R.dimen.help_text_inline_icon_size)
        setBounds(0, 0, dimensionPixelSize, dimensionPixelSize)
    }
}