---
appliesTo:
- {type: capability, id: delete-transaction}
- {type: entity, id: transfer, effect: removes}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/BaseTransactionProvider.kt#deleteTransaction
---

# Deleting a transfer deletes both of its sides

Deleting or voiding either side deletes or voids both. When the other side is a part of a split transaction, that part stays as an ordinary part of the split.
