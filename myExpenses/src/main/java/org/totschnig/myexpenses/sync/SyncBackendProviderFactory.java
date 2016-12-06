package org.totschnig.myexpenses.sync;

import android.accounts.Account;
import android.accounts.AccountManager;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.activity.ManageSyncBackends;

import java.util.ServiceLoader;

public abstract class SyncBackendProviderFactory {

  public static Optional<SyncBackendProvider> get(Account account, AccountManager accountManager) {
    return Stream.of(ServiceLoader.load(SyncBackendProviderFactory.class))
        .map(factory -> factory.from(account, accountManager))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  public Optional<SyncBackendProvider> from(Account account, AccountManager accountManager) {
    if (accountManager.getUserData(account, GenericAccountService.KEY_SYNC_PROVIDER_ID).equals(String.valueOf(getId()))) {
      return Optional.of(_fromAccount(account, accountManager));
    }
    return Optional.empty();
  }

  protected abstract SyncBackendProvider _fromAccount(Account account, AccountManager accountManager);

  public abstract int getId();

  public abstract String getLabel();

  public abstract void startSetup(ManageSyncBackends activity);
}
