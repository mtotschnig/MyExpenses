package org.totschnig.myexpenses.sync;

import org.totschnig.myexpenses.sync.json.TransactionChange;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public interface SyncBackend {
  boolean lock();
  List<TransactionChange> getChangeSetSince(long sequenceNumber);
  long writeChangeSet(List<TransactionChange> changeSet) throws IOException;
  boolean unlock();
}
