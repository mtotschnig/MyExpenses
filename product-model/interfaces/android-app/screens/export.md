---
entities:
- {entity: account, shows: [Name, Currency]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/dialog/ExportDialogFragment.kt
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/ExportViewModel.kt#startExport
---

# Export

Exporting the shown transactions to a file, optionally deleting them afterwards.
