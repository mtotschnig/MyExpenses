package org.totschnig.myexpenses.sync;

import android.support.annotation.NonNull;
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
    first.add(buildCreated().setUuid("random1").build());
    List<TransactionChange> second = new ArrayList<>();
    second.add(buildCreated().setUuid("random2").build());
    Pair<List<TransactionChange>, List<TransactionChange>> result = syncAdapter.mergeChangeSets(first, second);
    assertEquals(first, result.first);
    assertEquals(second, result.second);
  }

  @Test
  public void deleteInSameSetShouldTriggerRemovalOfRelatedChanges() {
    String uuid = "random";
    List<TransactionChange> first = new ArrayList<>();
    first.add(buildCreated().setUuid(uuid).build());
    first.add(buildDeleted().setUuid(uuid).build());
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
    first.add(buildUpdated().setUuid(uuid).build());
    List<TransactionChange> second = new ArrayList<>();
    second.add(buildDeleted().setUuid(uuid).build());
    Pair<List<TransactionChange>, List<TransactionChange>> result = syncAdapter.mergeChangeSets(first, second);
    assertEquals(0, result.first.size());
    assertEquals(second, result.second);
  }

  @Test
  public void updatesShouldBeMerged() {
    String uuid = "random";
    List<TransactionChange> first = new ArrayList<>();
    first.add(buildUpdated().setUuid(uuid).build());
    first.add(buildUpdated().setUuid(uuid).build());
    List<TransactionChange> second = new ArrayList<>();
    Pair<List<TransactionChange>, List<TransactionChange>> result = syncAdapter.mergeChangeSets(first, second);
    assertEquals(1, result.first.size());
  }

  @Test
  public void insertShouldNotBeHandledAsUpdate() {
    String uuid = "random";
    List<TransactionChange> first = new ArrayList<>();
    first.add(buildCreated().setUuid(uuid).build());
    first.add(buildUpdated().setUuid(uuid).build());
    List<TransactionChange> second = new ArrayList<>();
    Pair<List<TransactionChange>, List<TransactionChange>> result = syncAdapter.mergeChangeSets(first, second);
    assertEquals(2, result.first.size());
  }


  private TransactionChange.Builder buildCreated() {
    return buildWithTimestamp().setType(TransactionChange.Type.created);
  }

  @NonNull
  private TransactionChange.Builder buildWithTimestamp() {
    return TransactionChange.builder().setTimeStamp(System.currentTimeMillis());
  }

  private TransactionChange.Builder buildDeleted() {
    return buildWithTimestamp().setType(TransactionChange.Type.deleted);
  }

  private TransactionChange.Builder buildUpdated() {
    return buildWithTimestamp().setType(TransactionChange.Type.updated);
  }
}
