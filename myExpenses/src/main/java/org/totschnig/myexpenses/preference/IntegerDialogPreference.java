package org.totschnig.myexpenses.preference;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

/**
 * A preference for storing integer values
 */
public class IntegerDialogPreference extends DialogPreference {
  private boolean mValueSet;
  public IntegerDialogPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  public IntegerDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public IntegerDialogPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public IntegerDialogPreference(Context context) {
    super(context);
  }

  public void setValue(int value) {
    int oldValue = getValue();
    boolean changed = value != oldValue;
    if(changed || !this.mValueSet) {
      this.mValueSet = true;
      this.persistInt(value);
      if(changed) {
        this.notifyChanged();
      }
    }
  }
  public int getValue() {
    return getPersistedInt(0);
  }
}
