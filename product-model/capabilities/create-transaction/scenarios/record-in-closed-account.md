---
kind: validation
routes:
  app: Android app
steps:
- text: The User chooses New Transaction in a closed account
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: []}
  - {entity: account, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product says the account is closed and offers Reopen
  kind: condition
  entities:
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Closed account

## Trigger

The User chooses New Transaction while a closed account is selected.

## Outcome

Nothing is recorded; Reopen is offered.
