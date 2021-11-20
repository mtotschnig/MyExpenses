package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.os.Parcelable
import android.text.TextUtils
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.util.*

@Parcelize
class VersionInfo(val code: Int, val name: String): Parcelable {
    @IgnoredOnParcel
    val nameCondensed = name.replace(".", "")
    fun getChanges(ctx: Context): Array<String?>? {
        val res = ctx.resources
        val changesArray = when (nameCondensed) {
            "325" -> arrayOf(
                "${ctx.getString(R.string.contrib_feature_csv_import_label)}: ${
                    ctx.getString(
                        R.string.autofill
                    )
                }"
            )
            "330" -> arrayOf(
                "${ctx.getString(R.string.contrib_feature_csv_import_label)}: ${ctx.getString(R.string.tags)}",
                ctx.getString(R.string.active_tags)
            )
            "331" -> arrayOf(
                "${ctx.getString(R.string.menu_settings)} - ${ctx.getString(R.string.autofill)}: ${
                    ctx.getString(
                        R.string.ui_refinement
                    )
                }"
            )
            "332" -> arrayOf(
                "${ctx.getString(R.string.pref_translation_title)} : ${Locale("te").displayLanguage}",
                "${ctx.getString(R.string.currency)}: ${ctx.getString(R.string.ui_refinement)}"
            )
            "333" -> arrayOf(
                "${ctx.getString(R.string.pref_exchange_rate_provider_title)}: https://exchangerate.host",
                ctx.getString(R.string.pref_backup_cloud_summary)
            )
            "335" -> arrayOf(
                "${ctx.getString(R.string.debt_managment)}: 2.0"
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