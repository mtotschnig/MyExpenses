---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses Delete on a split transaction, ticks mark as void instead and confirms
  kind: actor
  actor: user
  entities:
  - {entity: split-transaction, effect: reads, facts: [Amount]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product marks the split transaction and its parts void
  kind: product
  actor: user
  entities:
  - {entity: split-transaction, effect: changes, from: Unreconciled, to: Void, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Void a split

## Trigger

The User marks a split transaction void instead of deleting it.

## Outcome

The split and its parts stay in the list as void.
