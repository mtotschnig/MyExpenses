---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Mark as unreconciled on a cleared transaction
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Amount]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product marks the transaction unreconciled
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: changes, from: Cleared, to: Unreconciled, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Mark as unreconciled

## Trigger

The User taps the status of a cleared transaction.

## Outcome

The transaction is unreconciled again.

## Edge cases

- A reconciled or void transaction keeps its status when tapped.
