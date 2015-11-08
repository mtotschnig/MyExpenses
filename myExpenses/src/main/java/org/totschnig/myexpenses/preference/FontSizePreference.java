package org.totschnig.myexpenses.preference;

import android.content.Context;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;

/**
 * Created by privat on 08.11.15.
 */
public class FontSizePreference extends DialogPreference {
  private boolean mValueSet;
  public FontSizePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  public FontSizePreference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public FontSizePreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public FontSizePreference(Context context) {
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
