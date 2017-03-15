package org.totschnig.myexpenses.preference;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;

import org.totschnig.myexpenses.R;

public class FontSizeDialogPreference extends IntegerDialogPreference {
  public FontSizeDialogPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init();
  }

  public FontSizeDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  public FontSizeDialogPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public FontSizeDialogPreference(Context context) {
    super(context);
    init();
  }

  private void init() {

    setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

      @Override
      public boolean onPreferenceChange(Preference arg0, Object arg1) {
        arg0.setSummary(getEntry(((Integer) arg1)));
        return true;
      }
    });
  }

  private String getEntry(int index) {
    String standard = getContext().getString(R.string.pref_ui_language_default);
    if (index ==0) return standard;
    else {
      return standard + " + " + index * 2 +"sp";
    }
  }

  @Override
  public CharSequence getSummary() {
    return getEntry(getValue());
  }

  public String[] getEntries() {
    return new String[] {
        getEntry(0),getEntry(1),getEntry(2),getEntry(3)
    };
  }
}
