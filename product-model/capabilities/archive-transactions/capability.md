---
domain: transactions
availability:
- {place: android-app}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/dialog/ArchiveDialogFragment.kt#ArchiveInfo
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/Archive.kt#archive
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryTransaction.kt#archive
---

# Archive transactions

The User packs the transactions of an account within a date range into one archive.
