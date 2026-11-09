---
appliesTo:
- {type: capability, id: browse-accounts}
- {type: capability, id: browse-transactions}
- {type: capability, id: export-transactions}
- {type: entity, id: account, facts: [Exclude from totals]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/DbConstants.kt
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/ExportViewModel.kt#startExport
---

# All accounts and groups of accounts leave out accounts excluded from totals

The totals, the list of all accounts, of one currency or of a group, and their export cover only accounts that are not excluded from totals.
