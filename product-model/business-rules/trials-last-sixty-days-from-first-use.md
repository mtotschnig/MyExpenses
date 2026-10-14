---
appliesTo:
- {type: capability, id: create-split-transaction}
- {type: capability, id: browse-budgets}
- {type: capability, id: import-csv-file}
- {type: entity, id: licence}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/util/licence/LicenceHandler.kt#recordUsage
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/util/licence/LicenceHandler.kt#getEndOfTrial
---

# A feature trial lasts 60 days from its first use

Each feature that can be tried has its own trial, which starts the first time the feature is used without a licence that covers it and ends 60 days later.
