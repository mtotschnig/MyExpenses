package org.totschnig.myexpenses.preference

import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceDialogFragmentCompat
import org.totschnig.myexpenses.adapter.FontSizeAdapter

class FontSizeDialogFragmentCompat : PreferenceDialogFragmentCompat() {
    var mClickedDialogEntryIndex: Int = 0
    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        val preference = getPreference() as FontSizeDialogPreference
        val selectedIndex = preference.value
        builder.setSingleChoiceItems(
            FontSizeAdapter(requireActivity()),
            selectedIndex
        ) { dialog: DialogInterface?, which: Int ->
            mClickedDialogEntryIndex = which
            this@FontSizeDialogFragmentCompat.onClick(dialog!!, DialogInterface.BUTTON_POSITIVE)
            dialog.dismiss()
        }
        builder.setPositiveButton(null, null)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult && mClickedDialogEntryIndex >= 0) {
            val preference = getPreference() as FontSizeDialogPreference
            if (preference.callChangeListener(mClickedDialogEntryIndex)) {
                preference.setValue(mClickedDialogEntryIndex)
            }
        }
    }

    companion object {
        fun newInstance(key: String?): FontSizeDialogFragmentCompat {
            val fragment = FontSizeDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.setArguments(bundle)
            return fragment
        }
    }
}
