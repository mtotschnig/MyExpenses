---
entities:
- {entity: account, shows: [Name, Currency]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/QifImport.kt#QifImport
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/dialog/QifImportDialogFragment.kt
---

# QIF import

Importing accounts, categories, parties and transactions from a QIF file.
