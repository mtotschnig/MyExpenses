---
appliesTo:
- {type: capability, id: delete-account}
- {type: capability, id: balance-account}
- {type: capability, id: export-transactions}
- {type: entity, id: transfer, effect: removes}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryAccount.kt#updateTransferPeersForTransactionDelete
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryAccount.kt#deleteAccount
---

# Deleting an account keeps the other side of its transfers

When an account is deleted, or reconciled or exported transactions are deleted from it, each transfer side left in another account becomes an ordinary transaction there, even in a closed account.
