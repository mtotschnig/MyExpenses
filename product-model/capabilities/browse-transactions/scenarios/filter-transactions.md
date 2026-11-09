---
kind: alternative
routes:
  app: Android app
steps:
- text: The User selects Quick search and toggles or configures filter criteria
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::transactions
- text: The Product applies the selected criteria using match-all logic to display matching transactions and their sums
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Category, Amount, Notes, Party, Payment method, Tags, Date]}
  - {entity: account, effect: reads, facts: []}
  - {entity: category, effect: reads, facts: []}
  - {entity: party, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/compose/filter/FilterDialog.kt#FilterDialog
---

# Quick search transactions

## Trigger

The User searches the list using Quick search.

## Outcome

Selected filter criteria are combined with match-all logic and immediately update the transaction list and sums.

## Edge cases

- Switching to Quick search is disabled when active criteria contain complex logic such as OR combinations, negations, or duplicate criterion types.
- Filters are retained per account until cleared.
