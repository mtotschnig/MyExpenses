---
entities:
- {entity: transaction, shows: [Account, Amount, Date, Value date, Category, Party, Payment method, Reference number, Notes, Tags, Original amount, Equivalent amount, Exported]}
- {entity: transfer, shows: [From account, To account, Amount, Received amount, Date, Value date, Category, Notes, Tags, Equivalent amount, From status, To status, Exported]}
- {entity: split-transaction, shows: [Account, Amount, Date, Value date, Party, Payment method, Reference number, Notes, Tags, Parts, Equivalent amount, Exported]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/dialog/TransactionDetailFragment.kt#TransactionDetailFragment
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/TransactionDetailViewModel.kt
---

# Transaction details

Everything kept about one transaction, transfer or split transaction.
