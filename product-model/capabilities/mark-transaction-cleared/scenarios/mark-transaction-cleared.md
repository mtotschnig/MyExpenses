---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Mark as cleared on an unreconciled transaction
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Amount]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product marks the transaction cleared
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: changes, from: Unreconciled, to: Cleared, facts: []}
  - {entity: account, effect: reads, facts: [Cleared total]}
  contexts:
    app:
      place: android-app::transactions
---

# Mark as cleared

## Trigger

The User taps the status of an unreconciled transaction.

## Outcome

The transaction is cleared and counts in the cleared total.

## Edge cases

- Status handles are shown only in accounts whose type supports reconciliation, on request.
