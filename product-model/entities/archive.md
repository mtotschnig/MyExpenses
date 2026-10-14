---
domain: transactions
relations:
- {entity: transaction, verb: contains, cardinality: one-to-many}
- {entity: split-transaction, verb: contains, cardinality: one-to-many}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/Archive.kt#archive
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/dialog/ArchiveDialogFragment.kt#ArchiveInfo
---

# Archive

The transactions of one account within a date range, packed into one entry that stands for them in the list.

## Information kept

- **Period** — the first and last day of the range, shown as its notes
- **Amount** — the sum of the packed transactions that are not void
- **Date** — the date of the latest packed transaction
- **Status** — the status the packed transactions share, or Unreconciled in an account without reconciliation
- **Transaction count** — how many transactions it holds
