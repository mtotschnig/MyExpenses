---
kind: alternative
routes:
  app: Android app
steps:
- text: The User enters a valid full-text search query string using search operators and confirms
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::transactions
- text: The Product searches transaction text fields using full-text search and displays matching transactions and their sums
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Notes, Party, Category, Payment method, Tags]}
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/compose/filter/FilterDialog.kt#FilterDialog
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/filter/FtsCriterion.kt#FtsCriterion
---

# Full-text search transactions

## Trigger

The User searches the list using Full-text search.

## Outcome

Transactions matching the full-text search query across text fields are listed with their sums.

## Edge cases

- Supported operators include space for AND, `-` for NOT, `OR`, `"..."` for phrases, `*` for prefixes, and `column:` prefixes.
