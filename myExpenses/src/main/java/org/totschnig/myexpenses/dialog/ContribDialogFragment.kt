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

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.core.view.isVisible
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eltos.simpledialogfragment.SimpleDialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.databinding.ContribDialogBinding
import org.totschnig.myexpenses.dialog.LicenceKeyDialogFragment.Companion.VALIDATION_SUCCESS
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.TextUtils.concatResStrings
import org.totschnig.myexpenses.util.TextUtils.getContribFeatureLabelsAsList
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.distrib.DistributionHelper.isGithub
import org.totschnig.myexpenses.util.distrib.DistributionHelper.isPlay
import org.totschnig.myexpenses.util.licence.AddOnPackage
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.util.licence.LicenceStatus
import org.totschnig.myexpenses.util.licence.Package
import org.totschnig.myexpenses.util.licence.ProfessionalPackage
import org.totschnig.myexpenses.util.tracking.Tracker
import java.io.Serializable
import javax.inject.Inject

class ContribDialogFragment : BaseDialogFragment(), View.OnClickListener,
    SimpleDialog.OnDialogResultListener {
    private var _binding: ContribDialogBinding? = null
    private val binding get() = _binding!!
    private var feature: ContribFeature? = null
    private var contribVisible = false
    private var extendedVisible = false
    private var singleVisible = false

    private val trialButton
        get() = binding.tryButton

    private val contribButton
        get() = binding.contribFeatureContainer.packageButton
    private val extendedButton
        get() = binding.extendedFeatureContainer.packageButton
    private val professionalButton
        get() = binding.professionalFeatureContainer.packageButton
    private val singleButton
        get() = binding.singleFeatureContainer.packageButton

    @State
    var selectedPackage: Package? = null

    @Inject
    lateinit var licenceHandler: LicenceHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StateSaver.restoreInstanceState(this, savedInstanceState)
        requireArguments().getString(ContribInfoDialogActivity.KEY_FEATURE)?.let {
            feature = ContribFeature.valueOf(it)
        }
        injector.inject(this)
        childFragmentManager.setFragmentResultListener(VALIDATION_SUCCESS, this) { _, _ ->
            contribActivity?.finish(false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        StateSaver.saveInstanceState(this, outState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = requireActivity() as ProtectedFragmentActivity
        val licenceStatus = licenceHandler.licenceStatus
        val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(ctx)
        _binding = ContribDialogBinding.inflate(LayoutInflater.from(builder.context))
        dialogView = binding.root

        //prepare trial card
        feature?.let { feature ->
            feature.buildTrialString(ctx, licenceHandler)?.also { trialString ->
                val removePhrase = feature.buildRemoveLimitation(requireContext(), true)
                if (licenceHandler.hasTrialAccessTo(feature)) {
                    binding.intro.text = removePhrase
                    binding.trialInfoCard.isVisible = true
                    binding.trialInfoCard.setOnClickListener(this)
                    trialButton.setOnClickListener(this)
                    binding.trialInfo.text = trialString
                    setSelectedForA11y(binding.trialInfoCard)
                } else {
                    binding.trialInfoCard.isVisible = false
                    binding.intro.text = TextUtils.concat(trialString, " ", removePhrase)
                }
            }
        } ?: run {
            binding.trialInfoCard.isVisible = false
            binding.intro.text = TextUtils.concat(
                *buildList {
                    if (isGithub) {
                        add(Utils.getTextWithAppName(context, R.string.dialog_contrib_text_1))
                        add(" ")
                    }
                    add(Utils.getTextWithAppName(context, R.string.dialog_contrib_text_2))
                }.toTypedArray()
            )
        }

        val contribFeatureLabelsAsList = getContribFeatureLabelsAsList(ctx, LicenceStatus.CONTRIB)
        val extendedFeatureLabelsAsList = getContribFeatureLabelsAsList(ctx, LicenceStatus.EXTENDED)

        //prepare CONTRIB section
        with(binding.contribFeatureContainer) {
            root.setBackgroundColorFromLicenceStatus(LicenceStatus.CONTRIB)
            if (licenceStatus == null && LicenceStatus.CONTRIB.covers(feature)) {
                contribVisible = true
                val contribList =
                    Utils.makeBulletList(ctx, contribFeatureLabelsAsList, R.drawable.ic_menu_done)
                packageFeatureList.text = contribList
                packageLabel.setText(R.string.contrib_key)
                packagePrice.text = licenceHandler.getFormattedPrice(Package.Contrib)
                root.setOnClickListener(this@ContribDialogFragment)
                contribButton.setOnClickListener(this@ContribDialogFragment)
            } else {
                root.isVisible = false
            }
        }

        //prepare EXTENDED section
        with(binding.extendedFeatureContainer) {
            root.setBackgroundColorFromLicenceStatus(LicenceStatus.EXTENDED)
            if (LicenceStatus.CONTRIB.greaterOrEqual(licenceStatus) &&
                LicenceStatus.EXTENDED.covers(feature)
            ) {
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
                packagePrice.text =
                    licenceHandler.getFormattedPrice(if (licenceStatus == null) Package.Extended else Package.Upgrade)
                root.setOnClickListener(this@ContribDialogFragment)
                extendedButton.setOnClickListener(this@ContribDialogFragment)
            } else {
                root.isVisible = false
            }
        }

        //prepare PROFESSIONAL section
        with(binding.professionalFeatureContainer) {
            packageIntro.isVisible = true
            if (isPlay) {
                packageIntro.setText(R.string.dialog_contrib_subscription_info_play)
            } else {
                packageIntro.isVisible = false
            }
            val lines = ArrayList<CharSequence>()
            root.setBackgroundColorFromLicenceStatus(LicenceStatus.PROFESSIONAL)
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
            lines.addAll(getContribFeatureLabelsAsList(ctx, LicenceStatus.PROFESSIONAL))
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
            feature?.let {
                root.setBackgroundColorFromLicenceStatus(it.licenceStatus)
                singleVisible = true
                packageLabel.setText(it.labelResId)
                packagePrice.text = licenceHandler.getFormattedPrice(getSinglePackage())
                root.setOnClickListener(this@ContribDialogFragment)
                singleButton.setOnClickListener(this@ContribDialogFragment)
                packageFeatureList.isVisible = false
                binding.singleFeatureInfo.isVisible = true
            } ?: run {
                root.isVisible = false
            }
        }

        //FOOTER
        if (isGithub) {
            binding.githubExtraInfo.isVisible = true
            binding.githubExtraInfo.text = getString(R.string.eu_vat_info)
            binding.githubSponsors.isVisible = true
            binding.githubSponsors.text = HtmlCompat.fromHtml(
                getString(R.string.github_sponsors, "https://github.com/sponsors/mtotschnig"),
                FROM_HTML_MODE_LEGACY
            )
            binding.githubSponsors.movementMethod = LinkMovementMethod.getInstance()
        }
        feature?.let {
            builder.setTitle(
                concatResStrings(
                    requireContext(),
                    ": ",
                    R.string.dialog_title_contrib_feature,
                    it.labelResId
                )
            )
        } ?: builder.setTitle(R.string.menu_contrib)

        builder.setView(dialogView)
            .setIcon(R.mipmap.ic_launcher_alt)
            .setPositiveButton( if (canTry) R.string.button_try else R.string.buy, null
            )

        if (licenceHandler.needsKeyEntry) {
            if (!licenceHandler.hasValidKey()) {
                builder.setNeutralButton(R.string.pref_enter_licence_title, null)
            }
        } else {
            builder.setNegativeButton(R.string.dialog_dismiss, null)
        }

        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                ?.setOnClickListener { onPositiveButtonClicked() }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                ?.setOnClickListener { onNeutralButtonClicked() }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                ?.setOnClickListener { onCancel(dialog) }
            if (savedInstanceState != null) {
                selectedPackage?.let {
                    updateButtons(
                        when (it) {
                            Package.Contrib -> contribButton
                            Package.Extended, Package.Upgrade -> extendedButton
                            is ProfessionalPackage -> {
                                updateProPrice(licenceStatus)
                                professionalButton
                            }
                            is AddOnPackage -> singleButton
                        }
                    )
                }
            }
        }
        return dialog
    }

    private val canTry
        get() = feature?.let { licenceHandler.hasTrialAccessTo(it)  } == true

    private fun View.setBackgroundColorFromLicenceStatus(licenceStatus: LicenceStatus) {
        setBackgroundColor(getColor(resources, licenceStatus.color, null))
    }

    private fun getSinglePackage(): AddOnPackage =
        AddOnPackage.values.find { it.feature == feature } ?: throw IllegalStateException()

    private val contribActivity: ContribInfoDialogActivity?
        get() = activity as ContribInfoDialogActivity?

    private fun onPositiveButtonClicked() {
        val ctx = contribActivity ?: return
        selectedPackage?.let {
            ctx.contribBuyDo(it)
            dismiss()
        } ?: run {
            if (canTry) {
                ctx.logEvent(Tracker.EVENT_CONTRIB_DIALOG_NEGATIVE, null)
                ctx.finish(false)
            } else {
                showSnackBar(R.string.select_package)
            }
        }
    }

    private fun onNeutralButtonClicked() {
        LicenceKeyDialogFragment().show(childFragmentManager, "LICENCE_KEY")
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (dialogTag == DIALOG_VALIDATE_LICENCE) {
            if (which == SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE) {
                prefHandler.putString(
                    PrefKey.NEW_LICENCE,
                    extras.getString(KEY_KEY)!!.trim()
                )
                prefHandler.putString(
                    PrefKey.LICENCE_EMAIL,
                    extras.getString(KEY_EMAIL)!!.trim()
                )
                contribActivity?.let {
                    it.setResult(Activity.RESULT_FIRST_USER)
                    it.finish()
                }
            }
        }
        return true
    }

    override fun onCancel(dialog: DialogInterface) {
        contribActivity?.let {
            it.logEvent(Tracker.EVENT_CONTRIB_DIALOG_CANCEL, null)
            it.finish(true)
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
        }
            ?: CrashHandler.report(java.lang.IllegalStateException("called without selectedPackage being professional"))
    }

    override fun onClick(v: View) {
        val licenceStatus = licenceHandler.licenceStatus
        if (v.id == R.id.trial_info_card || v == trialButton) {
            selectedPackage = null
            updateButtons(trialButton)
            setSelectedForA11y(binding.trialInfoCard)
        } else if (v.id == R.id.contrib_feature_container || v === contribButton) {
            selectedPackage = Package.Contrib
            updateButtons(contribButton)
            setSelectedForA11y(binding.contribFeatureContainer.root)
        } else if (v.id == R.id.extended_feature_container || v === extendedButton) {
            selectedPackage = if (licenceStatus == null) Package.Extended else Package.Upgrade
            updateButtons(extendedButton)
            setSelectedForA11y(binding.extendedFeatureContainer.root)
        } else if (v.id == R.id.single_feature_container || v === singleButton) {
            selectedPackage = getSinglePackage()
            updateButtons(singleButton)
            setSelectedForA11y(binding.extendedFeatureContainer.root)
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
                    setSelectedForA11y(binding.professionalFeatureContainer.root)
                    true
                }
                for (aPackage in proPackages.withIndex()) {
                    var title = licenceHandler.getFormattedPrice(aPackage.value)
                    if (title == null) title =
                        aPackage::class.java.simpleName //fallback if prices have not been loaded
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

    private fun updateButtons(selected: RadioButton) {
        trialButton.isChecked = trialButton === selected
        if (contribVisible) contribButton.isChecked = contribButton === selected
        if (extendedVisible) extendedButton.isChecked = extendedButton === selected
        if (singleVisible) singleButton.isChecked = singleButton === selected
        professionalButton.isChecked = professionalButton === selected
        (dialog as AlertDialog).getButton(DialogInterface.BUTTON_POSITIVE).setText(
            if (trialButton === selected) {
                if (feature == null) {
                    CrashHandler.throwOrReport("trialButton selected without feature")
                }
                R.string.button_try
            } else R.string.buy
        )
    }

    private fun setSelectedForA11y(packageContainer: View) {
        packageContainer.isSelected  = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val DIALOG_VALIDATE_LICENCE = "validateLicence"
        const val KEY_EMAIL = "email"
        const val KEY_KEY = "key"

        @JvmStatic
        fun newInstance(feature: String?, tag: Serializable?) = ContribDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ContribInfoDialogActivity.KEY_FEATURE, feature)
                putSerializable(ContribInfoDialogActivity.KEY_TAG, tag)
            }
        }
    }
}