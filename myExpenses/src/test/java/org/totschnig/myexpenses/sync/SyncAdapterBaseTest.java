package org.totschnig.myexpenses.sync;

import org.junit.Before;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.sync.json.TransactionChange;

import androidx.annotation.NonNull;

import static org.mockito.Mockito.mock;

class SyncAdapterBaseTest {
  SyncDelegate syncDelegate;

  @Before
  public void setup() {
    syncDelegate = new SyncDelegate(mock(CurrencyContext.class));
  }

  TransactionChange.Builder buildCreated() {
    return buildWithTimestamp().setType(TransactionChange.Type.created);
  }

  @NonNull
  private TransactionChange.Builder buildWithTimestamp() {
    return TransactionChange.builder().setCurrentTimeStamp();
  }

  TransactionChange.Builder buildDeleted() {
    return buildWithTimestamp().setType(TransactionChange.Type.deleted);
  }

  TransactionChange.Builder buildUpdated() {
    return buildWithTimestamp().setType(TransactionChange.Type.updated);
  }
}
