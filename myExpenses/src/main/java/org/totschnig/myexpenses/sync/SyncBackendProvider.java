package org.totschnig.myexpenses.sync;

import android.content.Context;

import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.json.TransactionChange;

import java.util.List;

public interface SyncBackendProvider {

  boolean lock();

  ChangeSet getChangeSetSince(long sequenceNumber, Context context);

  long writeChangeSet(List<TransactionChange> changeSet, Context context);

  boolean unlock();

  boolean isAvailable();

}
