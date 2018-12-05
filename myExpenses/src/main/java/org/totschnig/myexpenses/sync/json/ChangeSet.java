package org.totschnig.myexpenses.sync.json;


import org.totschnig.myexpenses.sync.SequenceNumber;

import java.util.ArrayList;
import java.util.List;

public class ChangeSet {

  public final SequenceNumber sequenceNumber;
  public final List<TransactionChange> changes;

  private ChangeSet(SequenceNumber sequenceNumber, List<TransactionChange> changes) {
    this.sequenceNumber = sequenceNumber;
    this.changes = changes;
  }

  public static ChangeSet create(SequenceNumber sequenceNumber, List<TransactionChange> changes) {
    return new ChangeSet(sequenceNumber, changes);
  }


  public static ChangeSet empty(SequenceNumber sequenceNumber) {
    return create(sequenceNumber, new ArrayList<>());
  }

  public static ChangeSet merge(ChangeSet changeset1, ChangeSet changeset2) {
    if (changeset1 == null || changeset2 == null) {
      return null;
    }
    List<TransactionChange> changes = new ArrayList<>();
    changes.addAll(changeset1.changes);
    changes.addAll(changeset2.changes);
    SequenceNumber max = SequenceNumber.max(changeset1.sequenceNumber, changeset2.sequenceNumber);
    return new ChangeSet(max, changes);
  }
}
