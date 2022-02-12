package org.totschnig.myexpenses.sync.json

import org.totschnig.myexpenses.sync.SequenceNumber
import org.totschnig.myexpenses.sync.SequenceNumber.Companion.max
import org.totschnig.myexpenses.sync.json.TransactionChange
import org.totschnig.myexpenses.sync.json.ChangeSet
import java.util.ArrayList

class ChangeSet private constructor(
    val sequenceNumber: SequenceNumber,
    val changes: List<TransactionChange>
) {
    companion object {
        fun create(sequenceNumber: SequenceNumber, changes: List<TransactionChange>): ChangeSet {
            return ChangeSet(sequenceNumber, changes)
        }

        fun empty(sequenceNumber: SequenceNumber): ChangeSet {
            return create(sequenceNumber, ArrayList())
        }

        fun merge(changeSet1: ChangeSet?, changeSet2: ChangeSet?): ChangeSet? {
            if (changeSet1 == null || changeSet2 == null) {
                return null
            }
            val changes: MutableList<TransactionChange> = ArrayList()
            changes.addAll(changeSet1.changes)
            changes.addAll(changeSet2.changes)
            val max = max(changeSet1.sequenceNumber, changeSet2.sequenceNumber)
            return ChangeSet(max, changes)
        }
    }
}