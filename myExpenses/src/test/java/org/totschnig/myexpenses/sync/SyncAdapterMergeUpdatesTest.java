package org.totschnig.myexpenses.sync;

import android.test.mock.MockContext;

import org.junit.Before;
import org.junit.Test;
import org.totschnig.myexpenses.sync.json.TransactionChange;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SyncAdapterMergeUpdatesTest {
  private SyncAdapter syncAdapter;

  @Before
  public void setup() {
    syncAdapter = new SyncAdapter(new MockContext(), true, true);
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowOnListWithOnlyOneElement() {
    List<TransactionChange> changes = new ArrayList<>();
    changes.add(TransactionChange.builder().setType(TransactionChange.Type.updated).build());
    syncAdapter.mergeUpdates(changes);
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowOnCreate() {
    List<TransactionChange> changes = new ArrayList<>();
    changes.add(TransactionChange.builder().setType(TransactionChange.Type.created).build());
    changes.add(TransactionChange.builder().setType(TransactionChange.Type.updated).build());
    syncAdapter.mergeUpdates(changes);
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowOnDelete() {
    List<TransactionChange> changes = new ArrayList<>();
    changes.add(TransactionChange.builder().setType(TransactionChange.Type.deleted).build());
    changes.add(TransactionChange.builder().setType(TransactionChange.Type.updated).build());
    syncAdapter.mergeUpdates(changes);
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowOnUpdatesWithDistinctUuids() {
    List<TransactionChange> changes = new ArrayList<>();
    changes.add(TransactionChange.builder().setType(TransactionChange.Type.updated).setUuid("one").build());
    changes.add(TransactionChange.builder().setType(TransactionChange.Type.updated).setUuid("two").build());
    syncAdapter.mergeUpdates(changes);
  }

  @Test
  public void shouldMergeTwoDifferentFields() {
    List<TransactionChange> changes = new ArrayList<>();
    String uuid = "one";
    String comment = "My comment";
    Long amount = 123L;
    changes.add(TransactionChange.builder().setType(TransactionChange.Type.updated).setUuid(uuid).setComment(comment).build());
    changes.add(TransactionChange.builder().setType(TransactionChange.Type.updated).setUuid(uuid).setAmount(amount).build());
    TransactionChange merge = syncAdapter.mergeUpdates(changes);
    assertEquals(comment, merge.comment());
    assertEquals(amount, merge.amount());
  }

  @Test
  public void lastChangeShouldOverride() {
    List<TransactionChange> changes = new ArrayList<>();
    String uuid = "one";
    String comment1 = "My earlier comment";
    String comment2 = "My later comment";
    Long later = System.currentTimeMillis();
    Long earlier = later - 10000;
    changes.add(TransactionChange.builder().setType(TransactionChange.Type.updated).setUuid(uuid).setTimeStamp(later).setComment(comment2).build());
    changes.add(TransactionChange.builder().setType(TransactionChange.Type.updated).setUuid(uuid).setTimeStamp(earlier).setComment(comment1).build());
    TransactionChange merge = syncAdapter.mergeUpdates(changes);
    assertEquals(comment2, merge.comment());
  }
}
