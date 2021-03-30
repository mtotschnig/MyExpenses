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
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import icepick.Icepick
import icepick.State
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.databinding.ContribDialogBinding
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.util.distrib.DistributionHelper.isGithub
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.licence.AddOnPackage
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.util.licence.LicenceStatus
import org.totschnig.myexpenses.util.licence.Package
import org.totschnig.myexpenses.util.licence.ProfessionalPackage
import org.totschnig.myexpenses.util.tracking.Tracker
import java.io.Serializable
import java.util.*
import javax.inject.Inject

class ContribDialogFragment : BaseDialogFragment(), DialogInterface.OnClickListener, View.OnClickListener {
    private var _binding: ContribDialogBinding? = null
    private val binding get() = _binding!!
    private var feature: ContribFeature? = null
    private var contribVisible = false
    private var extendedVisible = false
    private var singleVisible = false

    private val contribButton
        get() = binding.contribFeatureContainer.packageButton
    private val extendedButton
        get() = binding.extendedFeatureContainer.packageButton
    private val professionalButton
        get() = binding.professionalFeatureContainer.packageButton
    private val singleButton
        get() = binding.singleFeatureContainer.packageButton

    @JvmField
    @State
    var selectedPackage: Package? = null

    @Inject
    lateinit var licenceHandler: LicenceHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Icepick.restoreInstanceState(this, savedInstanceState)
        requireArguments().getString(ContribInfoDialogActivity.KEY_FEATURE)?.let {
            feature = ContribFeature.valueOf(it)
        }
        (requireActivity().application as MyApplication).appComponent.inject(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Icepick.saveInstanceState(this, outState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = requireActivity() as ProtectedFragmentActivity
        val licenceStatus = licenceHandler.licenceStatus
        val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(ctx, R.style.ContribDialogTheme)
        _binding = ContribDialogBinding.inflate(LayoutInflater.from(builder.context))
        dialogView = binding.root

        //prepare HEADER
        val message = feature?.let {
            val featureDescription = it.buildFullInfoString(ctx)
            val linefeed: CharSequence = HtmlCompat.fromHtml("<br>", FROM_HTML_MODE_LEGACY)
            val removePhrase = it.buildRemoveLimitation(activity, true)
            if (it.hasTrial()) {
                binding.usagesLeft.text = it.buildUsagesLefString(ctx, prefHandler)
                binding.usagesLeft.visibility = View.VISIBLE
            }
            TextUtils.concat(featureDescription, linefeed, removePhrase)
        } ?: Utils.getTextWithAppName(context, R.string.dialog_contrib_text_2).let {
            if (isGithub)
                TextUtils.concat(Utils.getTextWithAppName(context, R.string.dialog_contrib_text_1), " ", it)
            else it

        }
        binding.featureInfo.text = message
        val contribFeatureLabelsAsList = Utils.getContribFeatureLabelsAsList(ctx, LicenceStatus.CONTRIB)
        val extendedFeatureLabelsAsList = Utils.getContribFeatureLabelsAsList(ctx, LicenceStatus.EXTENDED)

        //prepare CONTRIB section
        with(binding.contribFeatureContainer) {
            root.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.premium_licence, null))
            if (licenceStatus == null && LicenceStatus.CONTRIB.covers(feature)) {
                contribVisible = true
                val contribList = Utils.makeBulletList(ctx, contribFeatureLabelsAsList, R.drawable.ic_menu_done)
                packageFeatureList.text = contribList
                packageLabel.setText(R.string.contrib_key)
                packagePrice.text = licenceHandler.getFormattedPrice(Package.Contrib)
                root.setOnClickListener(this@ContribDialogFragment)
                contribButton.setOnClickListener(this@ContribDialogFragment)
            } else {
                root.visibility = View.GONE
            }
        }

        //prepare EXTENDED section
        with(binding.extendedFeatureContainer) {
            root.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.extended_licence, null))
            if (LicenceStatus.CONTRIB.greaterOrEqual(licenceStatus) && LicenceStatus.EXTENDED.covers(feature)) {
                extendedVisible = true
                val lines = ArrayList<CharSequence>()
                if (contribVisible) {
                    lines.add(getString(R.string.all_contrib_key_features) + "\n+")
                } else if (licenceStatus == null && (feature?.isExtended == true)) {
                    lines.addAll(contribFeatureLabelsAsList)
                }
                lines.addAll(extendedFeatureLabelsAsList)
                packageFeatureList.text = Utils.makeBulletList(ctx, lines, R.drawable.ic_menu_done)
                packageLabel.setText(R.string.extended_key)
                packagePrice.text = licenceHandler.getFormattedPrice(if (licenceStatus == null) Package.Extended else Package.Upgrade)
                root.setOnClickListener(this@ContribDialogFragment)
                extendedButton.setOnClickListener(this@ContribDialogFragment)
            } else {
                root.visibility = View.GONE
            }
        }

        //prepare PROFESSIONAL section
        with(binding.professionalFeatureContainer) {
            val lines = ArrayList<CharSequence>()
            root.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.professional_licence, null))
            if (extendedVisible) {
                lines.add(getString(R.string.all_extended_key_features) + "\n+")
            } else if (feature?.isProfessional == true) {
                if (licenceStatus == null) {
                    lines.addAll(contribFeatureLabelsAsList)
                }
                if (LicenceStatus.CONTRIB.greaterOrEqual(licenceStatus)) {
                    lines.addAll(extendedFeatureLabelsAsList)
                }
            }
            lines.addAll(Utils.getContribFeatureLabelsAsList(ctx, LicenceStatus.PROFESSIONAL))
            packageFeatureList.text = Utils.makeBulletList(ctx, lines, R.drawable.ic_menu_done)
            packageLabel.setText(R.string.professional_key)
            packagePrice.text = licenceHandler.professionalPriceShortInfo
            val proPackages = licenceHandler.proPackages
            if (!contribVisible && !extendedVisible && proPackages.size == 1) {
                professionalButton.isChecked = true
                selectedPackage = proPackages[0]
            } else {
                root.setOnClickListener(this@ContribDialogFragment)
                professionalButton.setOnClickListener(this@ContribDialogFragment)
            }
        }

        //single FEATURE
        with(binding.singleFeatureContainer) {
            root.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.professional_licence, null))
            feature?.takeIf { licenceHandler.supportSingleFeaturePurchase(it) }?.let {
                singleVisible = true
                packageLabel.setText(it.getLabelResIdOrThrow(requireContext()))
                packagePrice.text = licenceHandler.getFormattedPrice(getSinglePackage())
                root.setOnClickListener(this@ContribDialogFragment)
                singleButton.setOnClickListener(this@ContribDialogFragment)
                packageFeatureList.visibility = View.GONE
                binding.singleFeatureInfo.visibility = View.VISIBLE
            } ?: run {
                root.visibility = View.GONE
            }
        }

        //FOOTER
        if (isGithub) {
            binding.githubExtraInfo.visibility = View.VISIBLE
            binding.githubExtraInfo.text = org.totschnig.myexpenses.util.TextUtils.concatResStrings(activity,
                    ". ", R.string.professional_key_fallback_info, R.string.eu_vat_info)
        }
        builder.setTitle(if (feature == null) R.string.menu_contrib else R.string.dialog_title_contrib_feature)
                .setView(dialogView)
                .setIcon(R.mipmap.ic_launcher_alt)
                .setPositiveButton(R.string.upgrade_now, null)
        if (feature?.let { licenceHandler.hasTrialAccessTo(it) } == true) {
            builder.setNeutralButton(R.string.dialog_contrib_no, this)
        }
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { onUpgradeClicked() }
            if (savedInstanceState != null) {
                selectedPackage?.let {
                    when (it) {
                        Package.Contrib -> contribButton
                        Package.Extended, Package.Upgrade -> extendedButton
                        is ProfessionalPackage -> {
                            updateProPrice(licenceStatus)
                            professionalButton
                        }
                        is AddOnPackage -> singleButton
                    }.isChecked = true
                }
            }
        }
        return dialog
    }

    private fun getSinglePackage() = when (feature) {
        ContribFeature.SPLIT_TEMPLATE -> AddOnPackage.SplitTemplate
        ContribFeature.HISTORY -> AddOnPackage.History
        ContribFeature.BUDGET -> AddOnPackage.Budget
        ContribFeature.OCR -> AddOnPackage.Ocr
        ContribFeature.WEB_UI -> AddOnPackage.WebUi
        else -> throw IllegalStateException()
    }

    private fun onUpgradeClicked() {
        val ctx = activity as ContribInfoDialogActivity? ?: return
        selectedPackage?.let {
            ctx.contribBuyDo(it)
            dismiss()
        } ?: run {
            showSnackbar(R.string.select_package)
        }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val ctx = activity as ContribInfoDialogActivity? ?: return
        when (which) {
            AlertDialog.BUTTON_NEUTRAL -> {
                ctx.logEvent(Tracker.EVENT_CONTRIB_DIALOG_NEGATIVE, null)
                ctx.finish(false)
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        val ctx = activity as ContribInfoDialogActivity?
        if (ctx != null) {
            ctx.logEvent(Tracker.EVENT_CONTRIB_DIALOG_CANCEL, null)
            ctx.finish(true)
        }
    }

    private fun updateProPrice(licenceStatus: LicenceStatus?) {
        (selectedPackage as? ProfessionalPackage)?.let { professionalPackage ->
            var formattedPrice = licenceHandler.getFormattedPrice(professionalPackage)
            if (formattedPrice != null) {
                if (licenceStatus === LicenceStatus.EXTENDED) {
                    licenceHandler.getExtendedUpgradeGoodyMessage(professionalPackage)?.let {
                        formattedPrice += " ($it)"
                    }
                }
                binding.professionalFeatureContainer.packagePrice.text = formattedPrice
            }
        } ?: CrashHandler.report(java.lang.IllegalStateException("called without selectedPackage being professional"))
    }

    override fun onClick(v: View) {
        val licenceStatus = licenceHandler.licenceStatus
        if (v.id == R.id.contrib_feature_container || v === contribButton) {
            selectedPackage = Package.Contrib
            updateButtons(contribButton)
        } else if (v.id == R.id.extended_feature_container || v === extendedButton) {
            selectedPackage = if (licenceStatus == null) Package.Extended else Package.Upgrade
            updateButtons(extendedButton)
        } else if (v.id == R.id.single_feature_container || v === singleButton) {
            selectedPackage = getSinglePackage()
            updateButtons(singleButton)
        } else {
            val proPackages = licenceHandler.proPackages
            if (proPackages.size == 1) {
                selectedPackage = proPackages[0]
                updateButtons(professionalButton)
            } else {
                val popup = PopupMenu(requireActivity(), professionalButton)
                popup.setOnMenuItemClickListener { item: MenuItem ->
                    selectedPackage = proPackages[item.itemId]
                    updateProPrice(licenceStatus)
                    updateButtons(professionalButton)
                    true
                }
                for (aPackage in proPackages.withIndex()) {
                    var title = licenceHandler.getFormattedPrice(aPackage.value)
                    if (title == null) title = aPackage::class.java.simpleName //fallback if prices have not been loaded
                    popup.menu.add(Menu.NONE, aPackage.index, Menu.NONE, title)
                }
                popup.setOnDismissListener {
                    if (selectedPackage !is ProfessionalPackage) {
                        professionalButton.isChecked = false
                    }
                }
                popup.show()
            }
        }
    }

    private fun updateButtons(selected: RadioButton?) {
        if (contribVisible) contribButton.isChecked = contribButton === selected
        if (extendedVisible) extendedButton.isChecked = extendedButton === selected
        if (singleVisible) singleButton.isChecked = singleButton === selected
        professionalButton.isChecked = professionalButton === selected
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(feature: String?, tag: Serializable?) = ContribDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ContribInfoDialogActivity.KEY_FEATURE, feature)
                putSerializable(ContribInfoDialogActivity.KEY_TAG, tag)
            }
        }
    }
}