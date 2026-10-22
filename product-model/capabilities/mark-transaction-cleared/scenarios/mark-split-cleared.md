---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses Mark as cleared on an unreconciled split transaction
  kind: actor
  actor: user
  entities:
  - {entity: split-transaction, effect: reads, facts: [Amount]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product marks the split transaction and its parts cleared
  kind: product
  actor: user
  entities:
  - {entity: split-transaction, effect: changes, from: Unreconciled, to: Cleared, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Mark a split as cleared

## Trigger

The User taps the status of an unreconciled split transaction.

## Outcome

The split and its parts are cleared.
