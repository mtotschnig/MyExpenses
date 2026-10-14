---
appliesTo:
- {type: entity, id: account, facts: [Current balance, Cleared total, Reconciled total]}
- {type: entity, id: transaction}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/DbConstants.kt
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/BaseTransactionProvider.kt
---

# An account's balance is its opening balance plus every entry that is not void

The current balance adds every transaction, transfer side and split transaction of the account that is not void to the opening balance; the cleared total counts only cleared and reconciled entries, the reconciled total only reconciled ones. Archived entries count through their archive.
