---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Undelete on a void transaction
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Amount]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product restores it as unreconciled
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: changes, from: Void, to: Unreconciled, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Restore a void transaction

## Trigger

The User chooses Undelete on a void transaction.

## Outcome

The transaction counts again as unreconciled.

## Edge cases

- The parts of a split and both sides of a transfer are restored with it.
- Several selected void transactions are restored at once and the count is reported.
