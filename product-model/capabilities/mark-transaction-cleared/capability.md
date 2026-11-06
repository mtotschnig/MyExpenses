---
domain: transactions
availability:
- {place: android-app}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BaseMyExpenses.kt#toggleCrStatus
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/MyExpensesViewModel.kt#toggleCrStatus
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/TransactionProvider.java
---

# Mark transaction cleared

The User marks an unreconciled transaction as cleared through the account.
