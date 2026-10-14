---
appliesTo:
- {type: capability, id: balance-account}
- {type: capability, id: export-transactions}
- {type: entity, id: account, facts: [Opening balance]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/ContentResolvingAndroidViewModel.kt#reset
---

# Deleting reconciled or exported entries keeps the balance

When balancing deletes the reconciled entries, the opening balance is raised by their sum. When exporting deletes the exported entries, the opening balance is raised by their sum or a helper transaction for it is recorded, as the User chooses.
