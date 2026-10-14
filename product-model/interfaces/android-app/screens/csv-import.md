---
entities:
- {entity: account, shows: [Name, Currency]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/CsvImportActivity.kt#CsvImportActivity
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/fragment/CsvImportParseFragment.kt
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/fragment/CsvImportDataFragment.kt
---

# CSV import

Importing transactions from a CSV file whose columns the person maps to fields.
