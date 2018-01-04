package org.totschnig.myexpenses.sync;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.annimon.stream.Stream;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.json.TransactionChange;
import org.totschnig.myexpenses.util.Result;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface SyncBackendProvider {

  boolean withAccount(Account account);

  boolean resetAccountData(String uuid);

  boolean lock();

  @NonNull
  ChangeSet getChangeSetSince(long sequenceNumber, Context context) throws IOException;

  long writeChangeSet(long lastSequenceNumber, List<TransactionChange> changeSet, Context context) throws IOException;

  boolean unlock();

  @NonNull
  Stream<AccountMetaData> getRemoteAccountList(android.accounts.Account account) throws IOException;

  Result setUp(String authToken);

  void tearDown();

  void storeBackup(Uri uri, String fileName) throws IOException;

  @NonNull
  List<String> getStoredBackups(android.accounts.Account account) throws IOException;

  InputStream getInputStreamForBackup(android.accounts.Account account, String backupFile) throws IOException;

  class SyncParseException extends Exception {
    SyncParseException(Exception e) {
      super(e);
    }
    SyncParseException(String message) {
      super(message);
    }
  }
}
