package org.totschnig.myexpenses.preference

import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.autofill.AutofillManager
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceDialogFragmentCompat
import com.google.android.material.textfield.TextInputLayout
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.Utils

class LegacyPasswordPreferenceDialogFragmentCompat : PreferenceDialogFragmentCompat(), TextWatcher,
    CompoundButton.OnCheckedChangeListener {
    private var boolProtectOrig = false
    private var boolProtect = false
    private var changePW = false
    private var strPass1: String? = null
    private var strPass2: String? = null
    private lateinit var password1: EditText
    private lateinit var password2: EditText
    private lateinit var password2Wrapper: TextInputLayout
    private lateinit var main: LinearLayout
    private lateinit var edit: LinearLayout

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            if (boolProtect && strPass1 != null && strPass1 == strPass2) {
                val hash = Utils.md5(strPass1)
                (requireContext().applicationContext as MyApplication).appComponent.prefHandler()
                    .putString(PrefKey.SET_PASSWORD, hash)
            }
            (getPreference() as LegacyPasswordPreference).setValue(boolProtect)
        }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        super.onClick(dialog, which)
        if (which == DialogInterface.BUTTON_POSITIVE) {
            requireContext().getSystemService(AutofillManager::class.java).commit()
        }
    }

    override fun onBindDialogView(view: View) {
        val preference = (getPreference() as LegacyPasswordPreference)
        password1 = view.findViewById(R.id.password1)
        password2 = view.findViewById(R.id.password2)
        val protect = view.findViewById<CheckBox>(R.id.performProtection)
        val change = view.findViewById<CheckBox>(R.id.changePassword)
        password2Wrapper = view.findViewById(R.id.password2Wrapper)
        main = view.findViewById(R.id.layoutMain)
        edit = view.findViewById(R.id.layoutPasswordEdit)
        boolProtectOrig = preference.getValue()
        boolProtect = boolProtectOrig
        protect.setChecked(boolProtect)
        if (boolProtect) {
            main.visibility = View.VISIBLE
            view.findViewById<View>(R.id.layoutChangePasswordCheckBox).visibility = View.VISIBLE
            edit.visibility = View.GONE
        }

        password1.addTextChangedListener(this)
        password2.addTextChangedListener(this)
        protect.setOnCheckedChangeListener(this)
        change.setOnCheckedChangeListener(this)
        super.onBindDialogView(view)
    }

    override fun afterTextChanged(s: Editable?) {
        validate()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        val id = buttonView.id
        if (id == R.id.performProtection) {
            main.visibility = if (isChecked) View.VISIBLE else View.GONE
            boolProtect = isChecked
            validate()
        } else if (id == R.id.changePassword) {
            edit.visibility = if (isChecked) View.VISIBLE else View.GONE
            changePW = isChecked
            validate()
        }
    }

    private fun validate() {
        val dlg = dialog
        val btn = (dlg as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
        if (!boolProtect || (boolProtectOrig && !changePW)) {
            btn.setEnabled(true)
            return
        }
        strPass1 = password1.getText().toString()
        strPass2 = password2.getText().toString()

        if (strPass1 == "") {
            btn.setEnabled(false)
        } else {
            if (strPass1 == strPass2) {
                password2Wrapper.setError(null)
                btn.setEnabled(true)
            } else {
                if (strPass2 != "") {
                    password2Wrapper.setError(getString(R.string.pref_password_not_equal))
                }
                btn.setEnabled(false)
            }
        }
    }

    companion object {
        fun newInstance(key: String) = LegacyPasswordPreferenceDialogFragmentCompat().apply {
            setArguments(Bundle(1).apply {
                putString(ARG_KEY, key)
            })
        }
    }
}
