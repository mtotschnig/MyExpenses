package org.totschnig.myexpenses.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;

import com.annimon.stream.Exceptional;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.sync.SyncBackendProvider.SyncParseException;
import org.totschnig.myexpenses.util.Result;

import java.io.Serializable;

public abstract class SyncBackendProviderFactory {

  public static Exceptional<SyncBackendProvider> get(Context context, Account account) {
    return Stream.of(ServiceLoader.load(context))
        .map(factory -> factory.from(context, account, AccountManager.get(context)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst()
        .orElse(Exceptional.of(new SyncParseException("No Provider found for account " + account.toString())));
  }

  private Optional<Exceptional<SyncBackendProvider>> from(Context context, Account account, AccountManager accountManager) {
    if (account.name.startsWith(getLabel())) {
      return Optional.of(Exceptional.of(() -> _fromAccount(context, account, accountManager)));
    }
    return Optional.empty();
  }

  public String buildAccountName(String extra) {
    return getLabel() + " - " + extra;
  }

  @NonNull
  protected abstract SyncBackendProvider _fromAccount(Context context, Account account, AccountManager accountManager) throws SyncParseException;

  public abstract String getLabel();

  public abstract void startSetup(FragmentActivity activity);

  boolean isEnabled(Context context) {
    return true;
  }

  public abstract int getId();

  public abstract Intent getRepairIntent(Activity activity);

  public abstract boolean startRepairTask(ManageSyncBackends activity, Intent data);

  public abstract Result handleRepairTask(Serializable mExtra);

  public abstract void init();
}
