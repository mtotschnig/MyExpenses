---
appliesTo:
- {type: capability, id: apply-plan-instance}
- {type: capability, id: execute-due-plans}
- {type: capability, id: cancel-plan-instance}
- {type: capability, id: reset-plan-instance}
- {type: entity, id: plan-instance, facts: [Transaction]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryTemplate.kt#loadTemplateIfInstanceIsOpen
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/TransactionDatabase.java
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/TemplatesListViewModel.kt#cancel
---

# An occurrence of a plan records at most one transaction

Applying records a transaction only for an open occurrence; cancelling or resetting an applied occurrence deletes its transaction.
