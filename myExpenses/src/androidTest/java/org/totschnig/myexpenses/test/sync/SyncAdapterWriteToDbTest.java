package org.totschnig.myexpenses.test.sync;

import android.content.ContentProviderOperation;
import android.test.mock.MockContext;

import org.junit.Before;
import org.junit.Test;
import org.totschnig.myexpenses.sync.SyncAdapter;
import org.totschnig.myexpenses.sync.json.TransactionChange;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SyncAdapterWriteToDbTest {
  private SyncAdapter syncAdapter;
  private ArrayList<ContentProviderOperation> ops;

  @Before
  public void setup() {
    syncAdapter = new SyncAdapter(new MockContext(), true, true);
    ops = new ArrayList<>();
  }

  @Test
  public void createChangeShouldBeCollectedAsInsertOperation() {
    TransactionChange change = TransactionChange.builder()
        .setType(TransactionChange.Type.created)
        .setUuid("any")
        .setTimeStamp(System.currentTimeMillis())
        .build();
    syncAdapter.collectOperations(change, ops);
    assertEquals(1, ops.size());
    assertTrue(ops.get(1).isInsert());
  }

}
