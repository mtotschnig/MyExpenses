package org.totschnig.myexpenses.preference;

import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TimePicker;

import androidx.preference.PreferenceDialogFragmentCompat;

public class TimePreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

  private TimePicker mTimePicker;

  @Override
  protected View onCreateDialogView(Context context) {
    final TimePreference preference = (TimePreference) getPreference();
    mTimePicker = ((TimePicker) super.onCreateDialogView(context));
    mTimePicker.setIs24HourView(DateFormat.is24HourFormat(context));
    mTimePicker.setCurrentHour(preference.getHour());
    mTimePicker.setCurrentMinute(preference.getMinute());
    return mTimePicker;
  }

  @Override
  public void onDialogClosed(boolean positiveResult) {
    final TimePreference preference = (TimePreference) getPreference();

    if (!positiveResult) {
      return;
    }
    mTimePicker.clearFocus();
    preference.setValue(100 * mTimePicker.getCurrentHour() + mTimePicker.getCurrentMinute());
  }

  public static TimePreferenceDialogFragmentCompat newInstance(String key) {
    TimePreferenceDialogFragmentCompat fragment = new TimePreferenceDialogFragmentCompat();
    Bundle bundle = new Bundle(1);
    bundle.putString(ARG_KEY, key);
    fragment.setArguments(bundle);
    return fragment;
  }
}
