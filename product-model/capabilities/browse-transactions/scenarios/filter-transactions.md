---
kind: alternative
routes:
  app: Android app
steps:
- text: The User sets a filter on one or more fields
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::transactions
- text: The Product shows only the transactions matching the category, amount, notes, status, party, payment method, tags, date or account filter, and their sums
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
---

# Filter the list

## Trigger

The User searches the list.

## Outcome

Only matching entries are shown, with their sums.

## Edge cases

- A filter is kept per account until it is cleared.
