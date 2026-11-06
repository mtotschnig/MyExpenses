---
appliesTo:
- {type: capability, id: import-csv-file}
- {type: entity, id: licence, facts: [CSV import trial]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/model/ContribFeature.kt#ContribFeature
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/CsvImportActivity.kt#importData
---

# CSV import needs an Extended licence or its trial

CSV import is available with an Extended or Professional licence, or during the 60-day trial that starts with the first import.
