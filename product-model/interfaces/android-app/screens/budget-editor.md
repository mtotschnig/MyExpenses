---
entities:
- {entity: budget, shows: [Title, Description, Account, Period, Start date, End date, Budgeted amount, Filters, Default budget], collects: [Title, Description, Account, Period, Start date, End date, Budgeted amount, Filters, Default budget]}
- {entity: account, shows: [Name, Currency]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BudgetEdit.kt#onFabClicked
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/BudgetEditViewModel.kt#saveBudget
---

# Budget form

Creating a budget or editing one.
