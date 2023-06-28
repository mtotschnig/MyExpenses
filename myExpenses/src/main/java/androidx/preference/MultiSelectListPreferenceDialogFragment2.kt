package androidx.preference

import android.content.DialogInterface
import android.os.Bundle

/**
 * a variant of [MultiSelectListPreferenceDialogFragmentCompat] that instead of persisting values,
 * allows to handle clicks in target fragment
 */
class MultiSelectListPreferenceDialogFragment2: MultiSelectListPreferenceDialogFragmentCompat() {
    interface OnClickListener {
        fun onClick(preference: String, values: Set<String>, which: Int)
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        (targetFragment as OnClickListener).onClick(
            requireArguments().getString(ARG_KEY)!!,
            mNewValues,
            which
        )
    }

    override fun onDialogClosed(positiveResult: Boolean) {}

    companion object {
        fun newInstance(key: String) = MultiSelectListPreferenceDialogFragment2().apply {
                arguments = Bundle(1).apply {
                    putString(ARG_KEY, key)
                }
            }
    }
}