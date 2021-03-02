package org.totschnig.myexpenses.preference;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.android.material.textfield.TextInputLayout;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.Utils;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceDialogFragmentCompat;

public class LegacyPasswordPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat
    implements TextWatcher, CompoundButton.OnCheckedChangeListener {

  private boolean boolProtectOrig, boolProtect, changePW = false;
  private String strPass1;
  private String strPass2;
  private EditText password1;
  private EditText password2;
  private TextInputLayout password2Wrapper;
  private LinearLayout main, edit;

  @Override
  public void onDialogClosed(boolean positiveResult) {

    if (positiveResult) {
      if (boolProtect && strPass1 != null && strPass1.equals(strPass2)) {
        String hash = Utils.md5(strPass1);
        PrefKey.SET_PASSWORD.putString(hash);
      }
      ((LegacyPasswordPreference) getPreference()).setValue(boolProtect);
    }
  }

  @Override
  protected void onBindDialogView(View view) {
    LegacyPasswordPreference preference = ((LegacyPasswordPreference) getPreference());
    password1 = view.findViewById(R.id.password1);
    password2 = view.findViewById(R.id.password2);
    CheckBox protect = view.findViewById(R.id.performProtection);
    CheckBox change = view.findViewById(R.id.changePassword);
    password2Wrapper = view.findViewById(R.id.password2Wrapper);
    main = view.findViewById(R.id.layoutMain);
    edit = view.findViewById(R.id.layoutPasswordEdit);
    boolProtectOrig = preference.getValue();
    boolProtect = boolProtectOrig;
    protect.setChecked(boolProtect);
    if (boolProtect) {
      main.setVisibility(View.VISIBLE);
      view.findViewById(R.id.layoutChangePasswordCheckBox).setVisibility(View.VISIBLE);
      edit.setVisibility(View.GONE);
    }

    password1.addTextChangedListener(this);
    password2.addTextChangedListener(this);
    protect.setOnCheckedChangeListener(this);
    change.setOnCheckedChangeListener(this);
    super.onBindDialogView(view);
  }

  @Override
  public void afterTextChanged(Editable s) {
    validate();
  }

  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {
  }

  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    int id = buttonView.getId();
    if (id == R.id.performProtection) {
      main.setVisibility(isChecked ? View.VISIBLE : View.GONE);
      boolProtect = isChecked;
      validate();
    } else if (id == R.id.changePassword) {
      edit.setVisibility(isChecked ? View.VISIBLE : View.GONE);
      changePW = isChecked;
      validate();
    }
  }

  private void validate() {
    Dialog dlg = getDialog();
    Button btn = ((AlertDialog) dlg).getButton(AlertDialog.BUTTON_POSITIVE);
    if (!boolProtect || (boolProtectOrig && !changePW)) {
      btn.setEnabled(true);
      return;
    }
    strPass1 = password1.getText().toString();
    strPass2 = password2.getText().toString();

    if (strPass1.equals("")) {
      btn.setEnabled(false);
    } else {
      if (strPass1.equals(strPass2)) {
        password2Wrapper.setError(null);
        btn.setEnabled(true);
      } else {
        if (!strPass2.equals("")) {
          password2Wrapper.setError(getString(R.string.pref_password_not_equal));
        }
        btn.setEnabled(false);
      }
    }
  }

  public static LegacyPasswordPreferenceDialogFragmentCompat newInstance(String key) {
    LegacyPasswordPreferenceDialogFragmentCompat fragment = new LegacyPasswordPreferenceDialogFragmentCompat();
    Bundle bundle = new Bundle(1);
    bundle.putString(ARG_KEY, key);
    fragment.setArguments(bundle);
    return fragment;
  }
}
