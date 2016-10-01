package org.totschnig.myexpenses.sync;

import android.support.v4.util.Pair;
import android.test.mock.MockContext;

import org.junit.Before;
import org.junit.Test;
import org.totschnig.myexpenses.sync.json.TransactionChange;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SyncAdapterMergeChangeSetsTest {
  private SyncAdapter syncAdapter;

  @Before
  public void setup() {
    syncAdapter = new SyncAdapter(new MockContext(), true, true);
  }

  @Test
  public void noConflictsShouldBeReturnedIdentical() {
    List<TransactionChange> first = new ArrayList<>();
    first.add(TransactionChange.builder().setType(TransactionChange.Type.created).build());
    List<TransactionChange> second = new ArrayList<>();
    second.add(TransactionChange.builder().setType(TransactionChange.Type.created).build());
    Pair<List<TransactionChange>, List<TransactionChange>> result = syncAdapter.mergeChangeSets(first, second);
    assertEquals(first, result.first);
    assertEquals(second, result.second);
  }

  @Test
  public void deleteInSameSetShouldTriggerRemovalOfRelatedChanges() {
    String uuid = "random";
    List<TransactionChange> first = new ArrayList<>();
    first.add(TransactionChange.builder().setType(TransactionChange.Type.created).setUuid(uuid).build());
    first.add(TransactionChange.builder().setType(TransactionChange.Type.deleted).setUuid(uuid).build());
    List<TransactionChange> second = new ArrayList<>();
    Pair<List<TransactionChange>, List<TransactionChange>> result = syncAdapter.mergeChangeSets(first, second);
    assertEquals(1, result.first.size());
    assertEquals(result.first.get(0),first.get(1));
    assertEquals(0, result.second.size());
  }

  @Test
  public void deleteInDifferentSetShouldTriggerRemovalOfRelatedChanges() {
    String uuid = "random";
    List<TransactionChange> first = new ArrayList<>();
    first.add(TransactionChange.builder().setType(TransactionChange.Type.updated).setUuid(uuid).build());
    List<TransactionChange> second = new ArrayList<>();
    second.add(TransactionChange.builder().setType(TransactionChange.Type.deleted).setUuid(uuid).build());
    Pair<List<TransactionChange>, List<TransactionChange>> result = syncAdapter.mergeChangeSets(first, second);
    assertEquals(0, result.first.size());
    assertEquals(second, result.second);
  }
}
