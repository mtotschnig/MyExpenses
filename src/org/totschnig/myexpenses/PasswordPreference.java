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

package org.totschnig.myexpenses;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.DialogPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PasswordPreference extends DialogPreference implements TextWatcher, OnCheckedChangeListener {
  
  private boolean boolProtectOrig, boolProtect, changePW = false;
  private String strPass1;
  private String strPass2;
  private EditText password1;
  private EditText password2;
  private CheckBox protect, change;
  private TextView error;
  private LinearLayout main, edit;
  
  public PasswordPreference(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    setDialogLayoutResource(R.layout.password_dialog);
    }
     
    public PasswordPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    setDialogLayoutResource(R.layout.password_dialog);
    }
    @Override
    protected void onDialogClosed(boolean positiveResult) {
      super.onDialogClosed(positiveResult);

      if (positiveResult) {
        if (boolProtect && strPass1 != null && strPass1.equals(strPass2)) {
          Editor editor = getEditor();
          String hash = Utils.md5(strPass1);
          editor.putString(MyApplication.PREFKEY_SET_PASSWORD, hash);
          editor.commit();
        }
        persistBoolean(boolProtect);
      }
    }
    @Override
    protected void onBindDialogView(View view) {
      password1    = (EditText) view.findViewById(R.id.password1);
      password2    = (EditText) view.findViewById(R.id.password2);
      protect = (CheckBox) view.findViewById(R.id.performProtection);
      change = (CheckBox) view.findViewById(R.id.changePassword);
      error        = (TextView) view.findViewById(R.id.passwordNoMatch);
      ((TextView) view.findViewById(R.id.password_warning)).setText(
          MyApplication.getInstance().isContribEnabled ?
              R.string.warning_password_contrib : R.string.warning_password_no_contrib);
      main = (LinearLayout) view.findViewById(R.id.layoutMain);
      edit = (LinearLayout) view.findViewById(R.id.layoutPasswordEdit);
      SharedPreferences pref = getSharedPreferences();
      boolProtectOrig = getPersistedBoolean(false);
      boolProtect= boolProtectOrig;
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
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
      switch (buttonView.getId()) {
      case R.id.performProtection:
        main.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        boolProtect = isChecked;
        validate();
        break;
      case R.id.changePassword:
        edit.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        changePW = isChecked;
        validate();
      }
    }
    private void validate() {
      Dialog dlg = getDialog();
      Button btn = ((AlertDialog)dlg).getButton(AlertDialog.BUTTON_POSITIVE);
      if (!boolProtect || (boolProtectOrig && !changePW)) {
        btn.setEnabled(true);
        return;
      }
      strPass1 = password1.getText().toString();
      strPass2 = password2.getText().toString();

      if (strPass1.equals("")) {
        error.setText(R.string.pref_password_empty);
        btn.setEnabled(false);
      } else if (strPass1.equals(strPass2)) {
          error.setText("");
          btn.setEnabled(true);
      } else {
        error.setText(R.string.pref_password_not_equal);
        btn.setEnabled(false);
      }
    }
}
