---
entities:
- {entity: account, shows: [Name, Currency, Account type, Color, Current balance, Goal or limit, Exclude from totals]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/compose/accounts/AccountList.kt
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/MyExpensesV2.kt
---

# Accounts

Every account with its balance, grouped by type, currency or flag, with totals per currency and in the home currency.
