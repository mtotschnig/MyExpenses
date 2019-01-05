package org.totschnig.myexpenses.preference;

import android.content.Context;
import android.support.v7.preference.ListPreference;
import android.util.AttributeSet;

import com.annimon.stream.Collectors;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.sync.GenericAccountService;

import java.util.ArrayList;
import java.util.List;

public class AccountPreference extends ListPreference {

  public static final String SYNCHRONIZATION_NONE = "_SYNCHRONIZATION_NONE_";

  public AccountPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    List<String> entries = GenericAccountService.getAccountsAsStream(context).map(account -> account.name).collect(Collectors.toList());
    List<String> entryValues = new ArrayList<>(entries);
    entries.add(0, getContext().getString(R.string.synchronization_none));
    entryValues.add(0, SYNCHRONIZATION_NONE);

    setEntries(entries.toArray(new String[0]));
    setEntryValues(entryValues.toArray(new String[0]));
  }

  public AccountPreference(Context context) {
    this(context, null);
  }

  @Override
  public int findIndexOfValue(String value) {
    int indexOfValue = super.findIndexOfValue(value);
    return indexOfValue > 0 ? indexOfValue : 0;
  }
}
