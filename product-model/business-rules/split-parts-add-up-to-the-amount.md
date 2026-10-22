---
appliesTo:
- {type: capability, id: create-split-transaction}
- {type: capability, id: edit-transaction}
- {type: entity, id: split-transaction, facts: [Parts, Amount]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/ExpenseEdit.kt#doSave
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/BaseTransactionDatabase.kt#SPLIT_PART_CR_STATUS_TRIGGER_CREATE
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/ImportDataViewModel.kt
---

# A split transaction's parts add up to its amount

A split transaction is saved only when nothing is left unsplit; its parts share its status.
