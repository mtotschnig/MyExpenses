---
appliesTo:
- {type: capability, id: link-transfer}
- {type: entity, id: transfer, effect: creates}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/MoreDbUtils.kt#linkTransfers
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/MyExpensesViewModel.kt#canLinkSelection
---

# Linking two transactions into a transfer drops their party and category

The two linked transactions must be in different accounts and have opposite amounts, unless their currencies differ; the transfer keeps neither party nor category.
