package org.totschnig.myexpenses;

import android.content.Context;
import  android.preference.DialogPreference;
import android.util.AttributeSet;

public class PasswordPreference extends DialogPreference {
  public PasswordPreference(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    setDialogLayoutResource(R.layout.password_dialog);
    }
     
    public PasswordPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    setDialogLayoutResource(R.layout.password_dialog);
    }
}
