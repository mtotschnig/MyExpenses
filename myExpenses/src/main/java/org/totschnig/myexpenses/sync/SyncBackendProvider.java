package org.totschnig.myexpenses.sync;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.annimon.stream.Stream;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.json.TransactionChange;

import java.io.IOException;
import java.util.List;

public interface SyncBackendProvider {

  boolean withAccount(Account account);

  boolean resetAccountData(String uuid);

  boolean lock();

  @NonNull
  ChangeSet getChangeSetSince(long sequenceNumber, Context context) throws IOException;

  long writeChangeSet(List<TransactionChange> changeSet, Context context) throws IOException;

  boolean unlock();

  Stream<AccountMetaData> getRemoteAccountList() throws IOException;

  boolean setUp();

  void tearDown();

  void storeBackup(Uri uri) throws IOException;

  @NonNull
  List<String> getStoredBackups();

  class SyncParseException extends Exception {
    SyncParseException(Exception e) {
      super(e);
    }
    SyncParseException(String message) {
      super(message);
    }
  }
}
