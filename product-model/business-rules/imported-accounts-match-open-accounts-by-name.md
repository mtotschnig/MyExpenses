---
appliesTo:
- {type: capability, id: import-qif-file}
- {type: capability, id: import-csv-file}
- {type: entity, id: account, effect: creates}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/ImportDataViewModel.kt#insertAccounts
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryAccount.kt#findAnyOpenByLabel
---

# An import puts transactions into the open account of the same name

An account named in an imported file is matched to an open account with that name; only accounts without a match are created.
