package org.totschnig.myexpenses.dialog

import android.app.Dialog
import android.graphics.drawable.Animatable2.AnimationCallback
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Pair
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.PreferenceActivity
import org.totschnig.myexpenses.databinding.MoreInfoBinding
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.myexpenses.util.ui.UiUtils
import java.util.Locale

class MoreInfoDialogFragment : DialogViewBinding<MoreInfoBinding>() {
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
        val nounIcons =
            listOf<CharSequence>(*resources.getStringArray(R.array.noun_icon_credits))
        //Warning TypedArray.close is API 31 -> We can not use use (tryWithResources)
        resources.obtainTypedArray(R.array.noun_icon_credits_keys).let {
            val height = UiUtils.dp2Px(32f, resources)
            val drawablePadding = UiUtils.dp2Px(8f, resources)
            var i = 0
            while (i < it.length()) {
                val textView = TextView(context)
                textView.gravity = Gravity.CENTER_VERTICAL
                val layoutParams =
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
                textView.layoutParams = layoutParams
                textView.compoundDrawablePadding = drawablePadding
                textView.text = nounIcons[i]
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    it.getResourceId(i, 0),
                    0,
                    0,
                    0
                )
                binding.nounIconsContainer.addView(textView)
                i++
            }
            it.recycle()
        }

        //noinspection SetTextI18n
        binding.copyRight.text = "© 2011 - ${BuildConfig.BUILD_DATE.year} Michael Totschnig"
        return builder.setTitle(R.string.pref_more_info_dialog_title)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    private fun ImageView.startAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (background as? AnimatedVectorDrawable)?.apply {
                start()
                registerAnimationCallback( object: AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable) {
                        start()
                    }
                })
            }
        }
    }

    override fun onStart() {
        super.onStart()
        binding.useanimationsHeart.startAnimation()
        binding.useanimationsNotificationv4.startAnimation()
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