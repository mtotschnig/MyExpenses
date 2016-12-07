package org.totschnig.myexpenses.sync;

import android.content.Context;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.json.TransactionChange;

import java.util.List;

public class WebDavBackendProvider implements SyncBackendProvider {

  public static final String KEY_WEB_DAV_URL = "webDavUrl";
  public static final String KEY_WEB_DAV_CERTIFICATE = "webDavCertificate";

  public WebDavBackendProvider() {
  }

  @Override
  public void withAccount(Account account) {

  }

  @Override
  public boolean lock() {
    return true;
  }

  @Override
  public ChangeSet getChangeSetSince(long sequenceNumber, Context context) {
    return null;
  }

  @Override
  public long writeChangeSet(List<TransactionChange> changeSet, Context context) {
    return 0;
  }

  @Override
  public boolean unlock() {
    return true;
  }

  @Override
  public boolean isAvailable() {
    return true;
  }
}
