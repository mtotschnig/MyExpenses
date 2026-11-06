---
entities:
- {entity: template, shows: [Title, Account, Kind, Amount, Category, Party, Payment method, Notes, Tags, To account, Parts, Default action, Recurrence, Plan start, Automatic execution, Execution in advance], collects: [Title, Account, Kind, Amount, Category, Party, Payment method, Notes, Tags, To account, Parts, Default action, Recurrence, Plan start, Automatic execution, Execution in advance]}
- {entity: account, shows: [Name, Currency]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/delegate/TransactionDelegate.kt#syncStateAndValidate
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/ExpenseEdit.kt
---

# Template form

Creating or editing a template and its plan.
