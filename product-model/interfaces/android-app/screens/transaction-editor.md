---
entities:
- {entity: transaction, shows: [Account, Amount, Date, Value date, Category, Party, Payment method, Reference number, Notes, Tags, Original amount, Equivalent amount], collects: [Account, Amount, Date, Value date, Category, Party, Payment method, Reference number, Notes, Tags, Original amount, Equivalent amount]}
- {entity: transfer, shows: [From account, To account, Amount, Received amount, Date, Value date, Category, Notes, Tags, Equivalent amount], collects: [From account, To account, Amount, Received amount, Date, Value date, Category, Notes, Tags, Equivalent amount]}
- {entity: split-transaction, shows: [Account, Amount, Date, Value date, Party, Payment method, Reference number, Notes, Tags, Parts, Equivalent amount], collects: [Account, Amount, Date, Value date, Party, Payment method, Reference number, Notes, Tags, Parts, Equivalent amount]}
- {entity: party, collects: [Name]}
- {entity: template, collects: [Title, Recurrence]}
- {entity: account, shows: [Name, Currency]}
- {entity: category, shows: [Name, Type]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/ExpenseEdit.kt#doSave
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/delegate/TransactionDelegate.kt#syncStateAndValidate
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/TransactionEditViewModel.kt#save
---

# Transaction form

Recording a new transaction, transfer or split transaction, or editing one, including its parts.
