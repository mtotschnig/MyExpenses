---
entities:
- {entity: account, shows: [Name, Currency, Opening balance, Current balance, Cleared total, Reconciled total]}
- {entity: transaction, shows: [Account, Amount, Date, Category, Party, Payment method, Reference number, Notes, Tags, Original amount, Equivalent amount]}
- {entity: transfer, shows: [From account, To account, Amount, Received amount, Date, Category, Notes, Tags, From status, To status]}
- {entity: split-transaction, shows: [Amount, Date, Party, Notes, Tags, Parts]}
- {entity: archive, shows: [Period, Amount, Date, Status, Transaction count], collects: [Period]}
- {entity: budget, shows: [Title, Budgeted amount, Spent, Available]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BaseMyExpenses.kt
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/compose/transactions/TransactionRenderer.kt
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/MyExpensesViewModel.kt
---

# Transactions

The transactions of the selected account, or of all accounts, one currency or one group of accounts, with the balance, sums, the default budget, and the actions on single or selected transactions.
