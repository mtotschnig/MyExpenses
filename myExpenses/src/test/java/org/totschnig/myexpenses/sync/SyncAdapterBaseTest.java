package org.totschnig.myexpenses.sync;

import android.content.Context;
import androidx.annotation.NonNull;

import org.junit.Before;
import org.mockito.Mockito;
import org.totschnig.myexpenses.sync.json.TransactionChange;

class SyncAdapterBaseTest {
  SyncAdapter syncAdapter;

  @Before
  public void setup() {
    syncAdapter = new SyncAdapter(Mockito.mock(Context.class), true, true);
  }

  protected TransactionChange.Builder buildCreated() {
    return buildWithTimestamp().setType(TransactionChange.Type.created);
  }

  @NonNull
  private TransactionChange.Builder buildWithTimestamp() {
    return TransactionChange.builder().setCurrentTimeStamp();
  }

  protected TransactionChange.Builder buildDeleted() {
    return buildWithTimestamp().setType(TransactionChange.Type.deleted);
  }

  protected TransactionChange.Builder buildUpdated() {
    return buildWithTimestamp().setType(TransactionChange.Type.updated);
  }
}
