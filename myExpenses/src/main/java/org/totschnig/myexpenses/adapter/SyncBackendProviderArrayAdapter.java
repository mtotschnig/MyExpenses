package org.totschnig.myexpenses.adapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.annimon.stream.Stream;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.sync.GenericAccountService;

import static android.content.Context.ACCOUNT_SERVICE;

public class SyncBackendProviderArrayAdapter extends ArrayAdapter<String> {
  boolean includeNoop = false;
  private Account[] accounts;

  public SyncBackendProviderArrayAdapter(FragmentActivity context, @LayoutRes int resource, boolean includeNoop) {
    super(context, resource);
    this.includeNoop = includeNoop;
    loadData();
  }

  public SyncBackendProviderArrayAdapter(FragmentActivity context, @LayoutRes int resource) {
    this(context, resource, false);
  }

  @NonNull
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    return super.getView(position, convertView, parent);
  }

  public void loadData() {
    clear();
    if (includeNoop) {
      add(getContext().getString(R.string.synchronization_none));
    }
    AccountManager accountManager = (AccountManager) getContext().getSystemService(ACCOUNT_SERVICE);
    accounts = accountManager.getAccountsByType(GenericAccountService.ACCOUNT_TYPE);
    Stream.of(accounts)
        .map(account -> account.name)
        .forEach(this::add);
  }
}
