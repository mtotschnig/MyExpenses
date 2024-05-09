package org.totschnig.myexpenses.preference;

import android.content.Context;
import android.util.AttributeSet;

import org.apache.commons.lang3.ArrayUtils;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.sync.GenericAccountService;

import androidx.preference.ListPreference;

public class AccountPreference extends ListPreference {

  public static final String SYNCHRONIZATION_NONE = "_SYNCHRONIZATION_NONE_";

  public AccountPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public AccountPreference(Context context) {
    this(context, null);
  }

  @Override
  public int findIndexOfValue(String value) {
    int indexOfValue = super.findIndexOfValue(value);
    return Math.max(indexOfValue, 0);
  }

  public void setData(Context context) {
    String[] accounts = GenericAccountService.getAccountNames(context);
    setEntries(ArrayUtils.insert(0, accounts, context.getString(androidx.preference.R.string.not_set)));
    setEntryValues(ArrayUtils.insert(0, accounts, SYNCHRONIZATION_NONE));
    notifyChanged();
  }
}
