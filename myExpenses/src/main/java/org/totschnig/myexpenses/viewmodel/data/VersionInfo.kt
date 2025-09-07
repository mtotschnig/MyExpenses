package org.totschnig.myexpenses.viewmodel.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.localizedQuote
import java.util.Locale

@Parcelize
class VersionInfo(val code: Int, val name: String) : Parcelable {
    constructor(packedInfo: String) : this(packedInfo.split(';'))
    private constructor(parts: List<String>) : this(parts[0].toInt(), parts[1])

    @IgnoredOnParcel
    val nameCondensed = name.replace(".", "")
    fun getChanges(ctx: Context): Array<String>? {
        val res = ctx.resources
        fun t(resId: Int) = ctx.getString(resId)
        val changesArray = when (nameCondensed) {
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

            "371" -> arrayOf(
                "${t(R.string.take_photo)}: ${t(R.string.ui_refinement)}"
            )

            "372" -> arrayOf(
                "${t(R.string.synchronization)}: Microsoft OneDrive"
            )

            "373" -> arrayOf(
                "${t(R.string.synchronization)}: ${t(R.string.stability_improvements)}",
                "${t(R.string.help_MyExpenses_title)}: ${t(R.string.ui_refinement)}"
            )

            "376" -> arrayOf(
                t(R.string.whats_new_376_1),
                t(R.string.whats_new_376_2),
                t(R.string.menu_transform_to_transfer),
                "${ctx.getString(R.string.export_to_format, "CSV")}: MS Excel"
            )

            "377" -> arrayOf(
                "${
                    ctx.getString(
                        R.string.export_to_format,
                        "CSV"
                    )
                }: ${t(R.string.menu_original_amount)}, ${t(R.string.menu_equivalent_amount)}",
                t(R.string.ui_refinement)
            )

            "379" -> arrayOf(
                "${t(R.string.menu_original_amount)}: ${t(R.string.ui_refinement)}"
            )

            "381" -> arrayOf(
                // @formatter:off
                "${t(R.string.menu_print)}: ${t(R.string.configuration)} (${t(R.string.paper_format)}, ${t(R.string.header_footer)})"
                // @formatter:on
            )

            "382" -> arrayOf(
                "${t(R.string.menu_search)}: ${t(R.string.ui_refinement)}",
                "${t(R.string.account_widget_configuration)}: ${t(R.string.buttons)}",
                ctx.getString(
                    R.string.whats_new_382,
                    ctx.localizedQuote(t(R.string.title_scan_receipt_feature)),
                    ctx.localizedQuote(t(R.string.widget_title_accounts))
                )
            )

            "383" -> arrayOf(
                "${t(R.string.menu_budget)} 4.0"
            )

            "385" -> arrayOf(
                "FinTS: ING-DiBa",
                // @formatter:off
                "${t(R.string.menu_print)}: ${t(R.string.configuration)} (${t(R.string.paper_format)}, ${t(R.string.orientation)}, ${t(R.string.title_font_size)})"
                // @formatter:on
            )

            "389" -> arrayOf(
                "${t(R.string.archive)}: ${t(R.string.ui_refinement)}",
                "${t(R.string.title_activity_debt_overview)}: ${t(R.string.ui_refinement)}",
            )

            "390" -> arrayOf(
                "${t(R.string.saving_goal)} / ${t(R.string.credit_limit)}: ${t(R.string.ui_refinement)}",
            )

            "391" -> arrayOf(
                "${t(R.string.archive)}: ${t(R.string.ui_refinement)}",
                "${t(R.string.menu_budget)}: ${t(R.string.ui_refinement)}",
            )

            "392" -> arrayOf(
                "${t(R.string.menu_budget)}: ${t(R.string.synchronization)}",
            )

            "393" -> arrayOf(
                "${t(R.string.pref_exchange_rate_provider_title)}: ${t(R.string.ui_refinement)}"
            )

            "394" -> arrayOf(
                "${t(R.string.menu_search)} 2.0",
                "${t(R.string.split_transaction)}: ${t(R.string.ui_refinement)}"
            )

            "395" -> arrayOf(
                t(R.string.whats_new_395),
                t(R.string.performance_enhancement)
            )

            "397" -> arrayOf(
                "${t(R.string.quick_search)} / ${t(R.string.complex_search)}"
            )

            "398" -> arrayOf(
                "${t(R.string.balance_sheet)} : ${t(R.string.whats_new_398)}"
            )

            "399" -> arrayOf(
                t(R.string.whats_new_399),
                "${t(R.string.pref_translation_title)} : ${Locale("bn").displayLanguage}"
            )

            "400" -> arrayOf(
                t(R.string.whats_new_400_1),
                t(R.string.whats_new_400_2),
                "${t(R.string.pref_translation_title)} : ${Locale("th").displayLanguage}",
                "${t(R.string.pref_translation_title)} : ${Locale("be").displayLanguage}"
            )

            "401" -> arrayOf(
                t(R.string.whats_new_401),
                "${t(R.string.menu_print)} : ${t(R.string.balance_sheet)}, ${t(R.string.title_activity_debt_overview)}.",
                "${t(R.string.price_history)} : ${t(R.string.pref_category_title_import)} / ${t(R.string.pref_category_title_export)}."
            )

            "404" -> arrayOf(
                "${t(R.string.payer_or_payee)}: ${t(R.string.ui_refinement)}"
            )

            else -> {
                //noinspection DiscouragedApi
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
            //noinspection DiscouragedApi
            val resId = res.getIdentifier("contributors_$nameCondensed", "array", ctx.packageName)
            if (resId != 0) {
                val contributorArray = res.getStringArray(resId)
                return changesArray.mapIndexed { index, s ->
                    s + (contributorArray[index].takeIf { it.isNotEmpty() }?.let {
                        " (${ctx.getString(R.string.contributed_by, it)})"
                    } ?: "")
                }.toTypedArray()
            }
        }
        return changesArray
    }

    @SuppressLint("DiscouragedApi")
    private fun resolveMoreInfo(ctx: Context, resPrefix: String, ): Int? {
        return ctx.resources.getIdentifier(
            resPrefix + nameCondensed,
            "string",
            ctx.packageName
        ).takeIf { it != 0 }
    }

    fun githubLink(context: Context) =
        resolveMoreInfo(context, "project_board_")?.let { context.getString(it) }

    fun mastodonLink(context: Context) =
        resolveMoreInfo(context, "version_more_info_")?.let { context.getString(it) }

    fun githubUrl(context: Context) = githubLink(context)?.let {
        (if (code < 740) "https://github.com/mtotschnig/MyExpenses/projects/" else "https://github.com/users/mtotschnig/projects/") + it
        }

    fun mastodonUrl(context: Context) = mastodonLink(context)?.let {
            "https://mastodon.social/@myexpenses/$it"
        }

}