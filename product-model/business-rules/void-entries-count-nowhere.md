---
appliesTo:
- {type: entity, id: transaction, effect: changes, to: Void}
- {type: entity, id: split-transaction, effect: changes, to: Void}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/DbConstants.kt
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/Archive.kt#archive
---

# Void entries are kept but counted nowhere

A void transaction or split transaction stays in the list but is left out of balances, sums, budgets and the amount of an archive.
