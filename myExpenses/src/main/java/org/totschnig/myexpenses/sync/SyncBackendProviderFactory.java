package org.totschnig.myexpenses.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.support.annotation.NonNull;

import com.annimon.stream.Exceptional;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.sync.SyncBackendProvider.SyncParseException;

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

  @NonNull
  protected abstract SyncBackendProvider _fromAccount(Context context, Account account, AccountManager accountManager) throws SyncParseException;

  public abstract String getLabel();

  public abstract void startSetup(ManageSyncBackends activity);

  boolean isEnabled(Context context) {
    return true;
  }
}
