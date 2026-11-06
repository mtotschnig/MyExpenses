---
domain: accounts
availability:
- {place: android-app}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BaseMyExpenses.kt#toggleExcludeFromTotals
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/MyExpensesViewModel.kt#setExcludeFromTotals
---

# Exclude account from totals

The User leaves an account out of the totals across accounts.
