---
kind: validation
routes:
  app: Android app
steps:
- text: The User chooses Remap on a selection that includes a transaction of a closed account
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Amount]}
  - {entity: account, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product says at least one of the selected transactions cannot be edited because it refers to a closed account
  kind: condition
  entities:
  - {entity: account, effect: reads, facts: []}
  - {entity: transaction, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Closed account

## Trigger

The selection contains a transaction of a closed account.

## Outcome

Nothing is remapped.
