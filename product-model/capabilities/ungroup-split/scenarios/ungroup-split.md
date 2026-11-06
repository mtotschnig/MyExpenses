---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Ungroup split on a split transaction and confirms
  kind: actor
  actor: user
  entities:
  - {entity: split-transaction, effect: reads, facts: [Parts]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product removes the split transaction and records each of its parts as an ordinary transaction
  kind: product
  actor: user
  entities:
  - {entity: split-transaction, effect: removes, from: Unreconciled}
  - {entity: transaction, effect: creates, to: Unreconciled, facts: [Account, Amount, Date, Category, Notes, Tags]}
  contexts:
    app:
      place: android-app::transactions
---

# Ungroup a split

## Trigger

The User chooses Ungroup split on a split transaction and confirms.

## Outcome

Each part is an ordinary transaction in the account and the split is gone.
