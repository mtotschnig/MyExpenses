package org.totschnig.myexpenses.sync;

import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.json.TransactionChange;

import java.util.List;

public interface SyncBackend {
  boolean lock();
  ChangeSet getChangeSetSince(long sequenceNumber);
  long writeChangeSet(List<TransactionChange> changeSet);
  boolean unlock();
}
