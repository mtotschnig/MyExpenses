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
import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import icepick.Icepick
import icepick.State
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.databinding.ContribDialogBinding
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.util.DistributionHelper.isGithub
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.util.licence.LicenceStatus
import org.totschnig.myexpenses.util.licence.Package
import org.totschnig.myexpenses.util.tracking.Tracker
import java.io.Serializable
import java.lang.IllegalStateException
import java.util.*
import javax.inject.Inject

class ContribDialogFragment : BaseDialogFragment(), DialogInterface.OnClickListener, View.OnClickListener {
    private var _binding: ContribDialogBinding? = null
    private val binding get() = _binding!!
    private var feature: ContribFeature? = null
    private var contribVisible = false
    private var extendedVisible = false
    private var singleVisisble = false

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
        val featureStringExtra = requireArguments().getString(ContribInfoDialogActivity.KEY_FEATURE)
        if (featureStringExtra != null) {
            feature = ContribFeature.valueOf(featureStringExtra)
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
        val message: CharSequence
        if (feature != null) {
            val featureDescription = feature!!.buildFullInfoString(ctx)
            val linefeed: CharSequence = Html.fromHtml("<br>")
            val removePhrase = feature!!.buildRemoveLimitation(activity, true)
            message = TextUtils.concat(featureDescription, linefeed, removePhrase)
            if (feature!!.hasTrial()) {
                binding.usagesLeft.text = feature!!.buildUsagesLefString(ctx, prefHandler)
                binding.usagesLeft.visibility = View.VISIBLE
            }
        } else {
            val contribText2 = Utils.getTextWithAppName(context, R.string.dialog_contrib_text_2)
            message = if (isGithub) {
                TextUtils.concat(Utils.getTextWithAppName(context, R.string.dialog_contrib_text_1), " ",
                        contribText2)
            } else {
                contribText2
            }
        }
        binding.featureInfo.text = message
        val contribFeatureLabelsAsList = Utils.getContribFeatureLabelsAsList(ctx, LicenceStatus.CONTRIB)
        val extendedFeatureLabelsAsList = Utils.getContribFeatureLabelsAsList(ctx, LicenceStatus.EXTENDED)

        //prepare CONTRIB section
        binding.contribFeatureContainer.root.setBackgroundColor(resources.getColor(R.color.premium_licence))
        if (licenceStatus == null && LicenceStatus.CONTRIB.covers(feature)) {
            contribVisible = true
            val contribList = Utils.makeBulletList(ctx, contribFeatureLabelsAsList, R.drawable.ic_menu_done)
            binding.contribFeatureContainer.packageFeatureList.text = contribList
        } else {
            binding.contribFeatureContainer.root.visibility = View.GONE
        }

        //prepare EXTENDED section
        binding.extendedFeatureContainer.root.setBackgroundColor(resources.getColor(R.color.extended_licence))
        if (LicenceStatus.CONTRIB.greaterOrEqual(licenceStatus) && LicenceStatus.EXTENDED.covers(feature)) {
            extendedVisible = true
            val lines = ArrayList<CharSequence>()
            if (contribVisible) {
                lines.add("""
    ${getString(R.string.all_contrib_key_features)}
    +
    """.trimIndent())
            } else if (licenceStatus == null && feature != null && feature!!.isExtended) {
                lines.addAll(contribFeatureLabelsAsList)
            }
            lines.addAll(extendedFeatureLabelsAsList)
            binding.extendedFeatureContainer.packageFeatureList.text = Utils.makeBulletList(ctx, lines, R.drawable.ic_menu_done)
        } else {
            binding.extendedFeatureContainer.root.visibility = View.GONE
        }

        //prepare PROFESSIONAL section
        val lines = ArrayList<CharSequence>()
        binding.professionalFeatureContainer.root.setBackgroundColor(resources.getColor(R.color.professional_licence))
        if (extendedVisible) {
            lines.add("""
    ${getString(R.string.all_extended_key_features)}
    +
    """.trimIndent())
        } else if (feature != null && feature!!.isProfessional) {
            if (licenceStatus == null) {
                lines.addAll(contribFeatureLabelsAsList)
            }
            if (LicenceStatus.CONTRIB.greaterOrEqual(licenceStatus)) {
                lines.addAll(extendedFeatureLabelsAsList)
            }
        }
        lines.addAll(Utils.getContribFeatureLabelsAsList(ctx, LicenceStatus.PROFESSIONAL))
        binding.professionalFeatureContainer.packageFeatureList.text = Utils.makeBulletList(ctx, lines, R.drawable.ic_menu_done)

        //single FEATURE
        if (feature?.licenceStatus === LicenceStatus.PROFESSIONAL) {
            singleVisisble = true
            binding.singleFeatureContainer.packageLabel.setText(feature!!.getLabelResIdOrThrow(requireContext()))
            binding.singleFeatureContainer.packagePrice.text = licenceHandler.getFormattedPrice(getSinglePackage())
            binding.singleFeatureContainer.root.setOnClickListener(this)
            singleButton.setOnClickListener(this)
        } else {
            binding.singleFeatureContainer.root.visibility = View.GONE
        }

        //FOOTER
        if (isGithub) {
            binding.githubExtraInfo.visibility = View.VISIBLE
            binding.githubExtraInfo.text = org.totschnig.myexpenses.util.TextUtils.concatResStrings(activity,
                    ". ", R.string.professional_key_fallback_info, R.string.eu_vat_info)
        }
        builder.setTitle(if (feature == null) R.string.menu_contrib else R.string.dialog_title_contrib_feature)
                .setView(dialogView)
                .setNeutralButton(R.string.button_label_close, this)
                .setIcon(R.mipmap.ic_launcher_alt)
                .setPositiveButton(R.string.upgrade_now, null)
        if (feature != null && feature!!.isAvailable(prefHandler)) {
            builder.setNegativeButton(R.string.dialog_contrib_no, this)
        }
        val dialog = builder.create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { onUpgradeClicked() } }
        if (contribVisible) {
            binding.contribFeatureContainer.packageLabel.setText(R.string.contrib_key)
            binding.contribFeatureContainer.packagePrice.text = licenceHandler.getFormattedPrice(Package.Contrib)
            binding.contribFeatureContainer.root.setOnClickListener(this)
            contribButton.setOnClickListener(this)
        }
        if (extendedVisible) {
            binding.extendedFeatureContainer.packageLabel.setText(R.string.extended_key)
            binding.extendedFeatureContainer.packagePrice.text = licenceHandler.getFormattedPrice(if (licenceStatus == null) Package.Extended else Package.Upgrade)
            binding.extendedFeatureContainer.root.setOnClickListener(this)
            extendedButton.setOnClickListener(this)
        }
        binding.professionalFeatureContainer.packageLabel.setText(R.string.professional_key)
        binding.professionalFeatureContainer.packagePrice.text = licenceHandler.professionalPriceShortInfo
        val proPackages = licenceHandler.proPackages
        if (!contribVisible && !extendedVisible && proPackages.size == 1) {
            professionalButton.isChecked = true
            selectedPackage = proPackages[0]
        } else {
            binding.professionalFeatureContainer.root.setOnClickListener(this)
            professionalButton.setOnClickListener(this)
        }
        if (savedInstanceState != null && selectedPackage != null) {
            updateProPrice(licenceStatus)
        }
        return dialog
    }

    private fun getSinglePackage() = when (feature) {
        ContribFeature.SPLIT_TEMPLATE -> Package.AddOn_SPLIT_TEMPLATE
        ContribFeature.HISTORY -> Package.AddOn_HISTORY
        ContribFeature.BUDGET -> Package.AddOn_BUDGET
        ContribFeature.OCR -> Package.AddOn_OCR
        ContribFeature.WEB_UI -> Package.AddOn_WEB_UI
        else -> throw IllegalStateException()
    }

    private fun onUpgradeClicked() {
        val ctx = activity as ContribInfoDialogActivity? ?: return
        if (selectedPackage != null) {
            ctx.contribBuyDo(selectedPackage!!)
            dismiss()
        } else {
            showSnackbar(R.string.select_package)
        }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val ctx = activity as ContribInfoDialogActivity? ?: return
        when (which) {
            AlertDialog.BUTTON_NEUTRAL -> {
                ctx.logEvent(Tracker.EVENT_CONTRIB_DIALOG_CANCEL, null)
                ctx.finish(true)
            }
            AlertDialog.BUTTON_NEGATIVE -> {
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
        var formattedPrice = licenceHandler.getFormattedPrice(selectedPackage)
        if (formattedPrice != null) {
            if (licenceStatus === LicenceStatus.EXTENDED) {
                val extendedUpgradeGoodieMessage = licenceHandler.getExtendedUpgradeGoodieMessage(selectedPackage)
                if (extendedUpgradeGoodieMessage != null) {
                    formattedPrice += String.format(" (%s)", extendedUpgradeGoodieMessage)
                }
            }
            binding.professionalFeatureContainer.packagePrice.text = formattedPrice
        }
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
                    selectedPackage = Package.values()[item.itemId]
                    updateProPrice(licenceStatus)
                    updateButtons(professionalButton)
                    true
                }
                for (aPackage in proPackages) {
                    var title = licenceHandler.getFormattedPrice(aPackage)
                    if (title == null) title = aPackage.name //fallback if prices have not been loaded
                    popup.menu.add(Menu.NONE, aPackage.ordinal, Menu.NONE, title)
                }
                popup.setOnDismissListener {
                    if (selectedPackage == null || !selectedPackage!!.isProfessional) {
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
        if (singleVisisble) singleButton.isChecked = singleButton === selected
        professionalButton.isChecked = professionalButton === selected
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(feature: String?, tag: Serializable?): ContribDialogFragment {
            val dialogFragment = ContribDialogFragment()
            val bundle = Bundle()
            bundle.putString(ContribInfoDialogActivity.KEY_FEATURE, feature)
            bundle.putSerializable(ContribInfoDialogActivity.KEY_TAG, tag)
            dialogFragment.arguments = bundle
            return dialogFragment
        }
    }
}