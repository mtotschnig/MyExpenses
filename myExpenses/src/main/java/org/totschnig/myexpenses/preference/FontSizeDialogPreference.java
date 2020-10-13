package org.totschnig.myexpenses.preference;

import android.content.Context;
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

    setOnPreferenceChangeListener((preference, newValue) -> {
      preference.setSummary(getEntry(getContext(), ((Integer) newValue)));
      return true;
    });
  }

  public static String getEntry(Context context, int index) {
    String standard = context.getString(R.string.pref_ui_language_default);
    if (index == 0) return standard;
    else {
      return standard + " + " + index * 10 + "%";
    }
  }

  @Override
  public CharSequence getSummary() {
    return getEntry(getContext(), getValue());
  }

  public static String[] getEntries(Context context) {
    return new String[]{
        getEntry(context, 0), getEntry(context, 1), getEntry(context, 2), getEntry(context, 3)
    };
  }
}
