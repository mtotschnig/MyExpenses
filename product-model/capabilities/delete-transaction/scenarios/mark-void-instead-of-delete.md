---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses Delete on a transaction, ticks mark as void instead and confirms
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Amount]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product marks the transaction void
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: changes, from: Unreconciled, to: Void, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Mark void instead

## Trigger

The User chooses Delete and ticks Do not delete transaction, mark as void instead.

## Outcome

The transaction stays in the list as void and is counted nowhere.
