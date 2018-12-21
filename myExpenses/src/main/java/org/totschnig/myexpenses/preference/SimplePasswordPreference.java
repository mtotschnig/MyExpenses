package org.totschnig.myexpenses.preference;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;

import org.totschnig.myexpenses.R;

public class SimplePasswordPreference extends DialogPreference {
  public SimplePasswordPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init();
  }
  public SimplePasswordPreference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  public SimplePasswordPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public SimplePasswordPreference(Context context) {
    super(context);
    init();
  }

  private void init() {
    setDialogLayoutResource(R.layout.simple_password_dialog);
  }

  @Nullable
  public String getValue() {
    return getPersistedString(null);
  }

  public void setValue(String value) {
    persistString(value);
  }
}
