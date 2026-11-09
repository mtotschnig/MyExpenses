---
appliesTo:
- {type: capability, id: export-transactions}
- {type: entity, id: transaction, facts: [Exported]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryAccount.kt#markAsExported
---

# Exported entries are marked as exported

Every exported entry is marked exported, so a later export can include only entries not yet exported.
