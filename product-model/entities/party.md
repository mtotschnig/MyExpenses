---
domain: transactions
relations:
- {entity: transaction, verb: is counterparty of, cardinality: one-to-many}
- {entity: split-transaction, verb: is counterparty of, cardinality: one-to-many}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryParty.kt
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/TransactionEditViewModel.kt#save
---

# Party

A payee or payer that transactions are made with.

## Information kept

- **Name** — the payee's or payer's name
