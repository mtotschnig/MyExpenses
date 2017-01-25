package org.totschnig.myexpenses.sync;

import android.support.annotation.NonNull;
import android.test.mock.MockContext;

import org.junit.Before;
import org.totschnig.myexpenses.sync.json.TransactionChange;

class SyncAdapterBaseTest {
  SyncAdapter syncAdapter;

  @Before
  public void setup() {
    syncAdapter = new SyncAdapter(new MockContext(), true, true);
  }

  protected TransactionChange.Builder buildCreated() {
    return buildWithTimestamp().setType(TransactionChange.Type.created);
  }

  @NonNull
  private TransactionChange.Builder buildWithTimestamp() {
    return TransactionChange.builder().setTimeStamp(System.currentTimeMillis());
  }

  protected TransactionChange.Builder buildDeleted() {
    return buildWithTimestamp().setType(TransactionChange.Type.deleted);
  }

  protected TransactionChange.Builder buildUpdated() {
    return buildWithTimestamp().setType(TransactionChange.Type.updated);
  }
}
