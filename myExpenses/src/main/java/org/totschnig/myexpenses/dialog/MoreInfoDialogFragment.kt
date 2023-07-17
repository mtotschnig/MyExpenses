package org.totschnig.myexpenses.dialog

import android.app.Dialog
import android.os.Bundle
import android.util.Pair
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.PreferenceActivity
import org.totschnig.myexpenses.databinding.MoreInfoBinding
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import java.util.Locale

class MoreInfoDialogFragment: DialogViewBinding<MoreInfoBinding>() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val builder = initBuilder {
            MoreInfoBinding.inflate(it)
        }
        binding.aboutVersionCode.text = DistributionHelper.getVersionInfo(context)
        binding.projectContainer.text = Utils.makeBulletList(
            context,
            Utils.getProjectDependencies(context)
                .map { project: Map<String, String> ->
                    val name = project["name"]
                    "${if (project.containsKey("extra_info")) "$name (${project["extra_info"]})" else name}, from ${project["url"]}, licenced under ${project["licence"]}"
                }.toList(), R.drawable.ic_menu_forward
        )
        val lines: List<CharSequence> = listOf(
            *resources.getStringArray(R.array.additional_credits),
            "${getString(R.string.translated_by)}: ${buildTranslationCredits()}"
        )
        binding.additionalContainer.text =
            Utils.makeBulletList(context, lines, R.drawable.ic_menu_forward)
        val iconLines =
            listOf<CharSequence>(*resources.getStringArray(R.array.additional_icon_credits))
        val ar = resources.obtainTypedArray(R.array.additional_icon_credits_keys)
        val height = UiUtils.dp2Px(32f, resources)
        val drawablePadding = UiUtils.dp2Px(8f, resources)
        var i = 0
        while (i < ar.length()) {
            val textView = TextView(context)
            textView.gravity = Gravity.CENTER_VERTICAL
            val layoutParams =
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
            textView.layoutParams = layoutParams
            textView.compoundDrawablePadding = drawablePadding
            textView.text = iconLines[i]
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                ar.getResourceId(i, 0),
                0,
                0,
                0
            )
            binding.additionalIconsContainer.addView(textView)
            i++
        }
        ar.recycle()
        //noinspection SetTextI18n
        binding.copyRight.text = "© 2011 - ${BuildConfig.BUILD_DATE.year} Michael Totschnig"
        return builder.setTitle(R.string.pref_more_info_dialog_title)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    private fun buildTranslationCredits() =
        resources.getStringArray(R.array.pref_ui_language_values)
            .map { lang ->
                val parts = lang.split("-".toRegex()).toTypedArray()
                Pair.create(
                    lang,
                    (requireActivity() as PreferenceActivity).getTranslatorsArrayResId(
                        parts[0],
                        if (parts.size == 2) parts[1].lowercase(Locale.ROOT) else null
                    )
                )
            }
            .filter { pair -> pair.second != 0 }
            .map { pair -> Pair.create(pair.first, resources.getStringArray(pair.second)) }
            .flatMap { pair -> pair.second.map { name -> Pair.create(name, pair.first) } }
            .groupBy({ it.first }, { it.second })
            .toSortedMap()
            .map { entry -> "${entry.key} (${entry.value.joinToString(", ")})" }
            .joinToString(", ")

}