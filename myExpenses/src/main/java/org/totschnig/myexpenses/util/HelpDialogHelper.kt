package org.totschnig.myexpenses.util

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.text.Html.ImageGetter
import android.text.TextUtils
import android.util.TypedValue
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.distrib.DistributionHelper

class HelpDialogHelper(val context: Context): ImageGetter {
    val resources: Resources = context.resources

    fun resolveTitle(
        item: String,
        prefix: String
    ): String =
        item.split(".").joinToString("/") {
            resolveStringOrThrowIf0((if (prefix == "form") "" else "menu_") + it)
        }

    fun resolveStringOrArray(
        resString: String,
        separateComponentsByLineFeeds: Boolean
    ): CharSequence? {
        val resIdString = resString.replace('.', '_')
        val arrayId = resolveArray(resIdString)
        return if (arrayId == 0) {
            val stringId = resolveString(resIdString)
            if (stringId == 0) {
                null
            } else {
                HtmlCompat.fromHtml(getStringSafe(stringId),
                    HtmlCompat.FROM_HTML_MODE_LEGACY, this, null)
            }
        } else {
            val linefeed: CharSequence = HtmlCompat.fromHtml("<br>",
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            val components = resources.getStringArray(arrayId)
                .filter { component -> !shouldSkip(component) }
                .map { component -> handle(component) }
            val resolvedComponents = ArrayList<CharSequence>()
            for (i in components.indices) {
                resolvedComponents.add(
                    HtmlCompat.fromHtml(
                        components[i],
                        HtmlCompat.FROM_HTML_MODE_LEGACY,
                        this,
                        null
                    )
                )
                if (i < components.size - 1) {
                    resolvedComponents.add(if (separateComponentsByLineFeeds) linefeed else " ")
                }
            }
            TextUtils.concat(*resolvedComponents.toTypedArray())
        }
    }

    private fun handle(component: String): String {
        return if (component.startsWith("popup")) {
            resolveName(component + "_intro") + " " + resources.getStringArray(
                resolveArray(
                    component + "_items"
                )
            ).joinToString(" ") {
                "<b>${resolveName(it)}</b>: ${resolveName(component + "_" + it)}"
            }
        } else {
            resolveName(component)
        }
    }

    private fun resolveName(name: String) = getStringSafe(resolveString(name))

    private fun shouldSkip(component: String): Boolean {
        when (component) {
            "help_ManageSyncBackends_drive" -> return DistributionHelper.isGithub
        }
        return false
    }

    @StringRes
    fun resolveString(resIdString: String): Int {
        return resolve(resIdString, "string")
    }

    /**
     * @throws Resources.NotFoundException if there is no resource for the given String. On the contrary, if the
     * String does exist in an alternate locale, but not in the default one,
     * the resulting exception is caught and empty String is returned.
     */
    @Throws(Resources.NotFoundException::class)
    fun resolveStringOrThrowIf0(resIdString: String): String {
        if (resIdString == "menu_categories_export") {
            return resources.getString(R.string.export_to_format, "QIF")
        }
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
    fun resolveArray(resIdString: String): Int {
        return resolve(resIdString, "array")
    }

    private fun resolve(resIdString: String, defType: String): Int {
        return resolve(resources, resIdString, defType, context.packageName)
    }

    private fun resolveSystem(
        resIdString: String,
        @Suppress("SameParameterValue") defType: String
    ): Int {
        return resolve(Resources.getSystem(), resIdString, defType, "android")
    }

    private fun resolve(
        resources: Resources,
        resIdString: String,
        defType: String,
        packageName: String
    ): Int {
        return resources.getIdentifier(resIdString, defType, packageName)
    }

    override fun getDrawable(name: String): Drawable? {
        val theme = context.theme
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
            val dimensionPixelSize =
                resources.getDimensionPixelSize(R.dimen.help_text_inline_icon_size)
            return ResourcesCompat.getDrawable(resources, resId, theme)?.apply {
                setBounds(0, 0, dimensionPixelSize, dimensionPixelSize)
            }
        } catch (e: Resources.NotFoundException) {
            null
        }
    }
}