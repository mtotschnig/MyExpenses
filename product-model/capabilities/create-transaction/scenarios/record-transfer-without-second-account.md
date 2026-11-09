---
kind: validation
routes:
  app: Android app
steps:
- text: The User chooses New Transfer while only one account exists
  kind: actor
  actor: user
  entities:
  - {entity: transfer, effect: reads, facts: []}
  - {entity: account, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product says that a transfer requires a second account and offers New account
  kind: condition
  entities:
  - {entity: account, effect: reads, facts: []}
  - {entity: transfer, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Only one account

## Trigger

The User chooses New Transfer while there is only one account.

## Outcome

No transfer is recorded; New account is offered.
