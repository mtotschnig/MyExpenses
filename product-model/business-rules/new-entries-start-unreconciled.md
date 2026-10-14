---
appliesTo:
- {type: capability, id: create-transaction}
- {type: capability, id: clone-transaction}
- {type: capability, id: apply-template}
- {type: capability, id: apply-plan-instance}
- {type: capability, id: execute-due-plans}
- {type: entity, id: transaction, effect: creates}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/data/TransactionEditData.kt
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/model/CrStatus.kt#CrStatus
---

# Entries recorded in the app start unreconciled

A transaction, split transaction, copy or transaction recorded from a template or plan starts unreconciled; only imports carry over the status in the file.
