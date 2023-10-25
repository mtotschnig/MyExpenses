package org.totschnig.myexpenses.fragment.preferences

import android.os.Bundle
import androidx.annotation.Keep
import androidx.preference.Preference
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.form.Input
import eltos.simpledialogfragment.form.SimpleFormDialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.preference.PopupMenuPreference
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.licence.Package

@Keep
class PreferencesContribFragment : BasePreferenceFragment(), SimpleDialog.OnDialogResultListener {

    override fun onResume() {
        super.onResume()
        configureContribPrefs()
    }

    override val preferencesResId = R.xml.preferences_contrib

    override fun onPreferenceTreeClick(preference: Preference) = when {
        super.onPreferenceTreeClick(preference) -> true
        matches(preference, PrefKey.CONTRIB_PURCHASE) -> {
            if (licenceHandler.isUpgradeable) {
                startActivity(ContribInfoDialogActivity.getIntentFor(preferenceActivity, null))
            } else {
                val proPackagesForExtendOrSwitch = licenceHandler.proPackagesForExtendOrSwitch
                if (proPackagesForExtendOrSwitch != null) {
                    if (proPackagesForExtendOrSwitch.size > 1) {
                        (preference as PopupMenuPreference).showPopupMenu(
                            { item ->
                                contribBuyDo(
                                    proPackagesForExtendOrSwitch[item.itemId],
                                    false
                                )
                                true
                            },
                            *proPackagesForExtendOrSwitch.map(licenceHandler::getExtendOrSwitchMessage)
                                .toTypedArray()
                        )
                    } else {
                        //Currently we assume that if we have only one item, we switch
                        contribBuyDo(proPackagesForExtendOrSwitch[0], true)
                    }
                }
            }
            true
        }
        matches(preference, PrefKey.NEW_LICENCE) -> {
            if (licenceHandler.hasValidKey()) {
                SimpleDialog.build()
                    .title(R.string.licence_key)
                    .msg(getKeyInfo())
                    .pos(R.string.button_validate)
                    .neg(R.string.menu_remove)
                    .show(
                        this,
                        DIALOG_MANAGE_LICENCE
                    )
            } else {
                val licenceKey = prefHandler.getString(PrefKey.NEW_LICENCE, "")
                val licenceEmail = prefHandler.getString(PrefKey.LICENCE_EMAIL, "")
                SimpleFormDialog.build()
                    .title(R.string.pref_enter_licence_title)
                    .fields(
                        Input.email(KEY_EMAIL)
                            .required().text(licenceEmail),
                        Input.plain(KEY_KEY).min(4)
                            .required().hint(R.string.licence_key).text(licenceKey)
                    )
                    .pos(R.string.button_validate)
                    .neut()
                    .show(
                        this,
                        DIALOG_VALIDATE_LICENCE
                    )
            }
            true
        }
        else -> false
    }

    private fun contribBuyDo(selectedPackage: Package, shouldReplaceExisting: Boolean) {
        context?.let {
            ContribInfoDialogActivity.getIntentFor(
                it,
                selectedPackage,
                shouldReplaceExisting
            )
        }?.let {
            startActivity(it)
        }
    }


    fun configureContribPrefs() {
        val contribPurchasePref = requirePreference<Preference>(PrefKey.CONTRIB_PURCHASE)
        val licenceKeyPref = findPreference<Preference>(PrefKey.NEW_LICENCE)
        if (licenceHandler.needsKeyEntry) {
            licenceKeyPref?.let {
                if (licenceHandler.hasValidKey()) {
                    it.title = getKeyInfo()
                    it.summary = TextUtils.concatResStrings(
                        requireActivity(), " / ",
                        R.string.button_validate, R.string.menu_remove
                    )
                } else {
                    it.setTitle(R.string.pref_enter_licence_title)
                    it.setSummary(R.string.pref_enter_licence_summary)
                }
            }
        } else {
            licenceKeyPref?.isVisible = false
        }
        val contribPurchaseTitle: String = licenceHandler.prettyPrintStatus(requireContext())
            ?: (getString(R.string.pref_contrib_purchase_title) + (if (licenceHandler.doesUseIAP)
                " (${getString(R.string.pref_contrib_purchase_title_in_app)})" else ""))
        var contribPurchaseSummary: String
        val licenceStatus = licenceHandler.licenceStatus
        if (licenceStatus == null && licenceHandler.addOnFeatures.isEmpty()) {
            contribPurchaseSummary = getString(R.string.pref_contrib_purchase_summary)
        } else {
            contribPurchaseSummary = if (licenceStatus?.isUpgradeable != false) {
                getString(R.string.pref_contrib_purchase_title_upgrade)
            } else {
                licenceHandler.getProLicenceAction(requireContext())
            }
            if (!android.text.TextUtils.isEmpty(contribPurchaseSummary)) {
                contribPurchaseSummary += "\n"
            }
            contribPurchaseSummary += getString(R.string.thank_you)
        }
        contribPurchasePref.summary = contribPurchaseSummary
        contribPurchasePref.title = contribPurchaseTitle
    }

    private fun getKeyInfo() =
        prefHandler.getString(PrefKey.LICENCE_EMAIL, "") + ": " + prefHandler.getString(PrefKey.NEW_LICENCE, "")

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        when (dialogTag) {
            DIALOG_VALIDATE_LICENCE -> {
                if (which == SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE) {
                    prefHandler.putString(
                        PrefKey.NEW_LICENCE,
                        extras.getString(KEY_KEY)!!.trim()
                    )
                    prefHandler.putString(
                        PrefKey.LICENCE_EMAIL,
                        extras.getString(KEY_EMAIL)!!.trim()
                    )
                    preferenceActivity.validateLicence()
                }
            }

            DIALOG_MANAGE_LICENCE -> {
                when (which) {
                    SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE ->
                        preferenceActivity.validateLicence()
                    SimpleDialog.OnDialogResultListener.BUTTON_NEGATIVE -> {
                        ConfirmationDialogFragment.newInstance(Bundle().apply {
                            putInt(
                                ConfirmationDialogFragment.KEY_TITLE,
                                R.string.dialog_title_information
                            )
                            putString(
                                ConfirmationDialogFragment.KEY_MESSAGE,
                                getString(R.string.licence_removal_information, 5)
                            )
                            putInt(
                                ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL,
                                R.string.menu_remove
                            )
                            putInt(
                                ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                                R.id.REMOVE_LICENCE_COMMAND
                            )
                        })
                            .show(parentFragmentManager, "REMOVE_LICENCE")
                    }
                }
            }
        }
        return true
    }

    companion object {
        const val DIALOG_VALIDATE_LICENCE = "validateLicence"
        const val DIALOG_MANAGE_LICENCE = "manageLicence"
        const val KEY_EMAIL = "email"
        const val KEY_KEY = "key"
    }
}