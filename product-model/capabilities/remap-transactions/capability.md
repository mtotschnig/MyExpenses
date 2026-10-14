---
domain: transactions
availability:
- {place: android-app}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/RemapHandler.kt
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/MyExpensesViewModel.kt#remap
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/MyExpensesViewModel.kt#cloneAndRemap
---

# Remap transactions

The User gives selected transactions a new category, party, payment method, account or date in one go, optionally on copies.
