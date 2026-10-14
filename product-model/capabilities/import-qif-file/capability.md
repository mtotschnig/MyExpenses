---
domain: file-import-export
availability:
- {place: android-app}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/QifImport.kt#QifImport
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/QifImportViewModel.kt#importData
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/ImportDataViewModel.kt#insertAccounts
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/ImportDataViewModel.kt#insertTransactions
---

# Import QIF file

The User imports accounts, categories, parties and transactions from a QIF file.
