---
domain: file-import-export
availability:
- {place: android-app}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/CsvImportActivity.kt#importData
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/CsvImportViewModel.kt#importData
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/fragment/CsvImportDataFragment.kt
---

# Import CSV file

The User imports transactions from a CSV file whose columns they map to fields. Needs an Extended licence or its trial.
