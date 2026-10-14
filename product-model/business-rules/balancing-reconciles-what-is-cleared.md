---
appliesTo:
- {type: capability, id: balance-account}
- {type: entity, id: transaction, effect: changes, to: Reconciled}
- {type: entity, id: split-transaction, effect: changes, to: Reconciled}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/MyExpensesViewModel.kt#balanceAccount
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BaseMyExpenses.kt#isMenuItemVisible
---

# Balancing an account reconciles exactly what is cleared

Balance account is offered for an open account whose type supports reconciliation and needs at least one cleared entry; it turns every cleared entry of the account into reconciled and leaves unreconciled ones as they are.
