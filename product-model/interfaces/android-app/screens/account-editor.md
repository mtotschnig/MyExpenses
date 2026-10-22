---
entities:
- {entity: account, shows: [Name, Description, Currency, Account type, Opening balance, Color, Goal or limit, Exchange rate, Dynamic exchange rate, Exclude from totals, Active tags], collects: [Name, Description, Currency, Account type, Opening balance, Color, Goal or limit, Exchange rate, Dynamic exchange rate, Exclude from totals, Active tags]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/AccountEdit.kt#saveState
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/AccountEditViewModel.kt#save
---

# Account

Creating a new account or editing an open one.
