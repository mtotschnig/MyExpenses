package org.totschnig.myexpenses.sync.json;


import java.util.ArrayList;
import java.util.List;

public class ChangeSet {
  public static long FAILED = -1;
  public static final ChangeSet empty = new ChangeSet(0, new ArrayList<>());
  public static final ChangeSet failed = new ChangeSet(FAILED, null);
  public final long sequenceNumber;
  public final List<TransactionChange> changes;

  private ChangeSet(long sequenceNumber, List<TransactionChange> changes) {
    this.sequenceNumber = sequenceNumber;
    this.changes = changes;
  }

  public static ChangeSet create(long sequenceNumber, List<TransactionChange> changes) {
    return new ChangeSet(sequenceNumber, changes);
  }

  public static ChangeSet merge(ChangeSet changeset1, ChangeSet changeset2) {
    if (changeset1.equals(failed) || changeset2.equals(failed)) {
      return failed;
    }
    List<TransactionChange> changes = new ArrayList<>();
    changes.addAll(changeset1.changes);
    changes.addAll(changeset2.changes);
    long max = Math.max(changeset1.sequenceNumber, changeset2.sequenceNumber);
    return new ChangeSet(max, changes);
  }
}
