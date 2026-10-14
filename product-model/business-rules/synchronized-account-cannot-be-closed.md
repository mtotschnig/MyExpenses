---
appliesTo:
- {type: capability, id: close-account}
- {type: entity, id: account, effect: changes, to: Closed}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BaseMyExpenses.kt#toggleAccountSealed
---

# An account linked to a synchronization backend cannot be closed

An account that is synchronized stays open; it has to be unlinked from its backend first.
