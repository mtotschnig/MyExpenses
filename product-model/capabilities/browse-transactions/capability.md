---
domain: transactions
availability:
- {place: android-app}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/MyExpensesViewModel.kt#getTransactions
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/DbConstants.kt
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/compose/transactions/TransactionRenderer.kt
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/compose/filter/FilterDialog.kt#FilterDialog
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/compose/filter/FilterHandler.kt#FilterHandler
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/filter/FtsCriterion.kt#FtsCriterion
---

# Browse transactions

The User reviews the transactions of an account or of a group of accounts, with balances and sums, and narrows them with filters.
