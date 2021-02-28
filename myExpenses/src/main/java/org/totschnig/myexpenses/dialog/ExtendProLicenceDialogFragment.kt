package org.totschnig.myexpenses.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import icepick.Icepick
import icepick.State
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity
import org.totschnig.myexpenses.adapter.MaterialSpinnerAdapter
import org.totschnig.myexpenses.databinding.ExtendProLicenceBinding
import org.totschnig.myexpenses.util.licence.LicenceHandler
import javax.inject.Inject

class ExtendProLicenceDialogFragment : DialogFragment(), DialogInterface.OnClickListener {
    private lateinit var binding: ExtendProLicenceBinding

    @Inject
    lateinit var licenceHandler: LicenceHandler

    @JvmField
    @State
    var selectedIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Icepick.restoreInstanceState(this, savedInstanceState)
        (requireActivity().application as MyApplication).appComponent.inject(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Icepick.saveInstanceState(this, outState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = ExtendProLicenceBinding.inflate(LayoutInflater.from(requireContext()))
        val proPackages = licenceHandler.proPackages.map(licenceHandler::getExtendOrSwitchMessage)
        val adapter = MaterialSpinnerAdapter(
                requireContext(),
                R.layout.support_simple_spinner_dropdown_item,
                proPackages)
        binding.actSelectLicence.setAdapter(adapter)
        binding.actSelectLicence.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ -> selectedIndex = position }
        val dialog = AlertDialog.Builder(requireContext())
                .setCancelable(false)
                .setMessage(requireArguments().getCharSequence(KEY_MESSAGE))
                .setPositiveButton(R.string.extend_validity, null)
                .setNegativeButton(android.R.string.cancel, null)
                .setView(binding.root).create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { onExtendClicked() } }
        return dialog
    }

    private fun onExtendClicked() {
        if (selectedIndex == -1) {
            binding.tilSelectLicence.error = getString(R.string.select_package)
        } else {
            startActivity(ContribInfoDialogActivity.getIntentFor(requireActivity(),
                    licenceHandler.proPackages[selectedIndex], false))
            dismiss()
        }
    }

    companion object {
        const val KEY_MESSAGE = "message"
        fun newInstance(message: CharSequence) = ExtendProLicenceDialogFragment().apply {
            arguments = Bundle().apply {
                putCharSequence(KEY_MESSAGE, message)
            }
        }
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {

    }
}