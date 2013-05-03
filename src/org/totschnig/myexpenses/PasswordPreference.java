package org.totschnig.myexpenses;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.preference.DialogPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class PasswordPreference extends DialogPreference implements TextWatcher {
  
  private String strPass1;
  private String strPass2;
  private EditText password1;
  private EditText password2;
  private TextView error;
  
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
      MyApplication app = MyApplication.getInstance();
      super.onDialogClosed(positiveResult);

      if (positiveResult && strPass1 != null && strPass1.equals(strPass2)) {
        String hash = Utils.md5(strPass1);
        persistString(hash);
        app.passwordHash = hash;
      }
    }
    @Override
    protected void onBindDialogView(View view) {
      password1    = (EditText) view.findViewById(R.id.password1);
      password2    = (EditText) view.findViewById(R.id.password2);
      error        = (TextView) view.findViewById(R.id.passwordNoMatch);

      password1.addTextChangedListener(this);
      password2.addTextChangedListener(this);
      super.onBindDialogView(view);
   }

    @Override
    public void afterTextChanged(Editable s) {
        Dialog dlg = getDialog();
        Button btn = ((AlertDialog)dlg).getButton(AlertDialog.BUTTON_POSITIVE);
        strPass1 = password1.getText().toString();
        strPass2 = password2.getText().toString();

        if (strPass1.equals(strPass2)) {
            error.setText("");
            btn.setEnabled(true);
        } else {
          error.setText(R.string.pref_password_not_equal);
          btn.setEnabled(false);
        }
    }
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    public void onTextChanged(CharSequence s, int start, int before, int count) {}
}
