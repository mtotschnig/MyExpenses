---
appliesTo:
- {type: capability, id: transform-to-split}
- {type: entity, id: split-transaction, effect: creates}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryTransaction.kt#groupToSplitTransaction
---

# Only transactions with one account, party and status are grouped into a split

Transform to split groups the selected transactions only when they share account, currency, party and status; the split takes their summed amount and the average of their dates, and the parts lose their own party and date.
