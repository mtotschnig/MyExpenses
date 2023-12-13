package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.os.Parcelable
import android.text.TextUtils
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.util.Locale

@Parcelize
class VersionInfo(val code: Int, val name: String) : Parcelable {
    @IgnoredOnParcel
    val nameCondensed = name.replace(".", "")
    fun getChanges(ctx: Context): Array<String?>? {
        val res = ctx.resources
        fun t(resId: Int) = ctx.getString(resId)
        val changesArray = when (nameCondensed) {
            "341" -> arrayOf(
                "${t(R.string.split_transaction)}: ${t(R.string.ui_refinement)}",
                "${t(R.string.split_parts_heading)}: ${t(R.string.menu_original_amount)}",
            )
            "342" -> arrayOf(
                "${t(R.string.title_webui)}: 2.0",
                )
            "343" -> arrayOf(
                "${t(R.string.customize)}: ${ctx.getString(R.string.export_to_format, "CSV")}",
                "${ctx.getString(R.string.export_to_format, "JSON")}: 2.0",
            )
            "344" -> arrayOf(
                "${t(R.string.icons_for_categories)}: 2.0"
            )
            "345" -> arrayOf(
                "${t(R.string.menu_budget)}: 3.0",
                "${t(R.string.title_webui)}: https",
            )
            "346" -> arrayOf(
                "${t(R.string.help_MyExpenses_title)}: ${t(R.string.redesign)}"
            )
            "347" -> arrayOf(
                "${t(R.string.synchronization)}: ${t(R.string.storage_description)}"
            )
            "348" -> arrayOf(
                "${t(R.string.pref_translation_title)} : ${Locale("nl").displayLanguage}",
            )
            "349" -> arrayOf(
                "${t(R.string.synchronization)} (WebDAV): ${t(R.string.menu_reconfigure)}",
                "${t(R.string.pref_translation_title)} : ${Locale("ur").displayLanguage}",
            )
            "350" -> arrayOf(
                "${t(R.string.encrypt_database)} (${t(R.string.experimental)})"
            )
            "351" -> arrayOf(
                "${t(R.string.debt_managment)}: ${t(R.string.ui_refinement)}"
            )
            "352" -> arrayOf(
                "${t(R.string.synchronization)}: ${t(R.string.pref_manage_categories_title)}"
            )
            "353" -> arrayOf(
                "${t(R.string.debt_managment)}: ${t(R.string.ui_refinement)}"
            )
            "354" -> arrayOf(
                "${t(R.string.pref_exchange_rate_provider_title)}: https://coinapi.io",
            )
            "359" -> arrayOf(
                t(R.string.whats_new_359),
                "${t(R.string.grand_total)}: ${t(R.string.menu_search)}: ${t(R.string.menu_equivalent_amount)}"
            )
            "360" -> arrayOf(
                "${t(R.string.help_ManageTemplates_plans_title)}: ${t(R.string.ui_refinement)}"
            )
            "362" -> arrayOf(
                "${t(R.string.settings_label)}: ${t(R.string.ui_refinement)}"
            )
            "363" -> arrayOf(
                "Multibanking (${Locale.GERMANY.displayCountry})"
            )
            "368" -> arrayOf(
                "${t(R.string.menu_distribution)}, ${t(R.string.menu_budget)}: ${t(R.string.ui_refinement)}"
            )
            else -> {
                val resId = res.getIdentifier(
                    "whats_new_$nameCondensed",
                    "array",
                    ctx.packageName
                ) //new based on name
                if (resId == 0) {
                    CrashHandler.report(Exception("missing change log entry for version $code"))
                    null
                } else {
                    res.getStringArray(resId)
                }
            }
        }
        if (changesArray != null) {
            val resId =
                res.getIdentifier("contributors_$nameCondensed", "array", ctx.packageName)
            if (resId != 0) {
                val contributorArray = res.getStringArray(resId)
                val resultArray = arrayOfNulls<String>(changesArray.size)
                for (i in changesArray.indices) {
                    resultArray[i] = changesArray[i].toString() +
                            if (contributorArray.size <= i || TextUtils.isEmpty(contributorArray[i])) "" else String.format(
                                " (%s)",
                                ctx.getString(R.string.contributed_by, contributorArray[i])
                            )
                }
                return resultArray
            }
        }
        return changesArray
    }

}