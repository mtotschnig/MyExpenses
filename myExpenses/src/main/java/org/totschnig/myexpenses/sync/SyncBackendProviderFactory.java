package org.totschnig.myexpenses.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.support.annotation.NonNull;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.util.AcraHelper;

public abstract class SyncBackendProviderFactory {

  public static Optional<SyncBackendProvider> get(Context context, Account account) {
    return Stream.of(ServiceLoader.load(context))
        .map(factory -> factory.from(context, account, AccountManager.get(context)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  private Optional<SyncBackendProvider> from(Context context, Account account, AccountManager accountManager) {
    if (account.name.startsWith(getLabel())) {
      try {
        return Optional.of(_fromAccount(context, account, accountManager));
      } catch (SyncBackendProvider.SyncParseException e) {
        AcraHelper.report(e);
      }
    }
    return Optional.empty();
  }

  @NonNull
  protected abstract SyncBackendProvider _fromAccount(Context context, Account account, AccountManager accountManager) throws SyncBackendProvider.SyncParseException;

  public abstract String getLabel();

  public abstract void startSetup(ManageSyncBackends activity);

  boolean isEnabled(Context context) {
    return true;
  }
}
