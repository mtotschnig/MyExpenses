---
appliesTo:
- {type: capability, id: create-template}
- {type: capability, id: edit-template}
- {type: entity, id: template, facts: [Amount, Default action, Automatic execution]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/delegate/TransactionDelegate.kt#syncStateAndValidate
---

# A template without amount is edited before it records anything

A template with no amount is saved with the default action Apply and edit, and a plan with no amount does not execute automatically.
