---
domain: transactions
availability:
- {place: android-app}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BaseMyExpenses.kt#createRow
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/ExpenseEdit.kt#doSave
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/delegate/SplitDelegate.kt
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryTransaction.kt#createSplitTransaction
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/util/licence/LicenceHandler.kt#recordUsage
---

# Create split transaction

The User records one payment divided into parts for different categories or other accounts. Needs a Contrib licence or its trial.
