package org.totschnig.myexpenses.sync;

import android.content.Context;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.json.TransactionChange;

import java.io.IOException;
import java.util.List;

public interface SyncBackendProvider {

  boolean withAccount(Account account);

  boolean lock();

  ChangeSet getChangeSetSince(long sequenceNumber, Context context) throws IOException;

  long writeChangeSet(List<TransactionChange> changeSet, Context context) throws IOException;

  boolean unlock();

  List<AccountMetaData> getRemoteAccountList() throws IOException;

  class SyncParseException extends Exception {
    SyncParseException(Exception e) {
      super(e);
    }
  }
}
