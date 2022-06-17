package org.totschnig.myexpenses.preference

import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceDialogFragmentCompat
import com.google.android.material.textfield.TextInputEditText
import org.totschnig.myexpenses.R

class SimplePasswordDialogFragmentCompat : PreferenceDialogFragmentCompat() {
    private lateinit var passwordInput: TextInputEditText
    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        passwordInput = view.findViewById(R.id.passwordEdit)
        val currentValue = (preference as SimplePasswordPreference).value
        if (currentValue != null) {
            passwordInput.setText(currentValue)
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        val preference = preference as SimplePasswordPreference
        if (!positiveResult) {
            return
        }
        preference.value = passwordInput.text.toString()
    }

    companion object {
        @JvmStatic
        fun newInstance(key: String) = SimplePasswordDialogFragmentCompat().apply {
                arguments = Bundle(1).apply {
                    putString(ARG_KEY, key)
                }
            }
    }
}