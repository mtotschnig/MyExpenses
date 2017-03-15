package org.totschnig.myexpenses.preference;

import android.content.Context;
import android.support.v7.preference.ListPreference;
import android.util.AttributeSet;

import com.annimon.stream.Stream;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.sync.GenericAccountService;

public class AccountPreference extends ListPreference {

  public AccountPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    CharSequence[] accounts = entries();
    setEntries(accounts);
    setEntryValues(accounts);
  }

  public AccountPreference(Context context) {
    this(context, null);
  }

  private CharSequence[] entries() {
    return Stream.concat(
        Stream.of(getContext().getString(R.string.synchronization_none)),
        GenericAccountService.getAccountsAsStream().map(account -> account.name))
        .toArray(size -> new String[size]);
  }
}
