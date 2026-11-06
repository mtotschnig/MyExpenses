---
kind: validation
routes:
  app: Android app
steps:
- text: The User chooses Balance account
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product says there are no cleared transactions to reconcile
  kind: condition
  entities:
  - {entity: account, effect: reads, facts: [Cleared total]}
  - {entity: transaction, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Nothing cleared

## Trigger

The User chooses Balance account while nothing is cleared.

## Outcome

Nothing changes.
