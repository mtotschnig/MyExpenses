package org.totschnig.myexpenses.sync;

import org.junit.Before;
import org.junit.Test;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.sync.json.TransactionChange;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class SyncAdapterMergeUpdatesTest extends SyncAdapterBaseTest {
  private SyncDelegate syncDelegate;
  @Before
  public void setUp() {
    syncDelegate = new SyncDelegate(mock(CurrencyContext.class));
  }

  @Test
  public void shouldReturnSameElement() {
    List<TransactionChange> changes = new ArrayList<>();
    final TransactionChange change = buildUpdated().setUuid("random").build();
    changes.add(change);
    TransactionChange merge = syncDelegate.mergeUpdates(changes);
    assertEquals(change, merge);
  }

  @Test
  public void shouldThrowOnEmptyList() {
    List<TransactionChange> changes = new ArrayList<>();
    mergeUpdatesAndExpectIllegalStateExpection(changes);
  }

  @Test
  public void shouldThrowOnDelete() {
    List<TransactionChange> changes = new ArrayList<>();
    changes.add(buildDeleted().setUuid("random").build());
    changes.add(buildUpdated().setUuid("random").build());
    mergeUpdatesAndExpectIllegalStateExpection(changes);
  }

  @Test
  public void shouldThrowOnUpdatesWithDistinctUuids() {
    List<TransactionChange> changes = new ArrayList<>();
    changes.add(buildUpdated().setUuid("one").build());
    changes.add(buildUpdated().setUuid("two").build());
    mergeUpdatesAndExpectIllegalStateExpection(changes);
  }

  private void mergeUpdatesAndExpectIllegalStateExpection(List<TransactionChange> changes) {
    try {
      syncDelegate.mergeUpdates(changes);
      fail("Expected IllegalStateEception to be thrown");
    } catch (IllegalStateException expected) {
      //expected
    }
  }

  @Test
  public void shouldMergeTwoDifferentFields() {
    List<TransactionChange> changes = new ArrayList<>();
    String uuid = "one";
    String comment = "My comment";
    Long amount = 123L;
    changes.add(buildUpdated().setUuid(uuid).setComment(comment).build());
    changes.add(buildUpdated().setUuid(uuid).setAmount(amount).build());
    TransactionChange merge = syncDelegate.mergeUpdates(changes);
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
    TransactionChange merge = syncDelegate.mergeUpdates(changes);
    assertEquals(comment2, merge.comment());
  }
}
