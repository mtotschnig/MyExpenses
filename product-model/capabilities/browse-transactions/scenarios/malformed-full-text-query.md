---
kind: validation
routes:
  app: Android app
steps:
- text: The User enters a full-text search query with malformed syntax
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::transactions
- text: The Product indicates an invalid search query syntax error and prevents applying the search
  kind: product
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::transactions
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/compose/filter/FilterDialog.kt#FilterDialog
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/filter/FtsCriterion.kt#isMalformedFtsQuery
---

# Reject malformed full-text query

## Trigger

The User inputs a full-text search query containing unclosed quotes or invalid operators.

## Outcome

The Product displays a syntax error message and disables the action to apply the search until the query syntax is corrected.
