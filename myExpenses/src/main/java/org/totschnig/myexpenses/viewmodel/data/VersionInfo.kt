package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.os.Parcelable
import android.text.TextUtils
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.getDisplayNameForScript
import java.util.*

@Parcelize
class VersionInfo(val code: Int, val name: String) : Parcelable {
    @IgnoredOnParcel
    val nameCondensed = name.replace(".", "")
    fun getChanges(ctx: Context): Array<String?>? {
        val res = ctx.resources
        fun t(resId: Int) = ctx.getString(resId)
        val changesArray = when (nameCondensed) {
            "325" -> arrayOf(
                "${t(R.string.contrib_feature_csv_import_label)}: ${t(R.string.autofill)}"
            )
            "330" -> arrayOf(
                "${t(R.string.contrib_feature_csv_import_label)}: ${t(R.string.tags)}",
                t(R.string.active_tags)
            )
            "331" -> arrayOf(
                "${t(R.string.menu_settings)} - ${t(R.string.autofill)}: ${t(R.string.ui_refinement)}"
            )
            "332" -> arrayOf(
                "${t(R.string.pref_translation_title)} : ${Locale("te").displayLanguage}",
                "${t(R.string.currency)}: ${t(R.string.ui_refinement)}"
            )
            "333" -> arrayOf(
                "${t(R.string.pref_exchange_rate_provider_title)}: https://exchangerate.host",
                t(R.string.pref_backup_cloud_summary)
            )
            "335" -> {
                val scripts = arrayOf("Han", "Deva", "Jpan", "Kore").joinToString {
                    getDisplayNameForScript(ctx, it)
                }
                arrayOf(
                    "${t(R.string.debt_managment)}: 2.0",
                    "${t(R.string.pref_category_title_export)}: JSON",
                    "${t(R.string.title_scan_receipt_feature)} ($scripts)"
                )
            }
            "337" -> arrayOf(
                "${t(R.string.synchronization)} - ${t(R.string.setup)}: ${t(R.string.ui_refinement)}"
            )
            "338" -> arrayOf(
                t(R.string.whats_new_338),
                "${t(R.string.menu_budget)}: ${t(R.string.ui_refinement)}"
            )
            "339" -> arrayOf(
                t(R.string.dialog_title_purge_backups),
                "${t(R.string.pref_perform_share_title)}: HTTP"
            )
            "340" -> arrayOf(
                t(R.string.whats_new_340),
                "${t(R.string.bug_fixes)}: ${t(R.string.synchronization)}"
            )
            else -> {
                val resId = res.getIdentifier(
                    "whats_new_$nameCondensed",
                    "array",
                    ctx.packageName
                ) //new based on name
                if (resId == 0) {
                    CrashHandler.reportWithFormat(
                        "missing change log entry for version %d",
                        code
                    )
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