---
appliesTo:
- {type: capability, id: create-template}
- {type: capability, id: edit-template}
- {type: entity, id: template, facts: [Title]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/delegate/TransactionDelegate.kt#syncStateAndValidate
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/TransactionEditViewModel.kt#maybeCreateInitialPlan
---

# A template has a title

A template is saved only with a title; one recorded together with a transaction is titled after its party, category or notes when none is given.
