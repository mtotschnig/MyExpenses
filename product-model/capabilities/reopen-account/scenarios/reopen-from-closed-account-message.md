---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses a new entry in a closed account
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product says the account is closed and offers Reopen
  kind: product
  actor: user
  entities:
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
- text: The User chooses Reopen
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::transactions
- text: The Product reopens the account
  kind: product
  actor: user
  entities:
  - {entity: account, effect: changes, from: Closed, to: Open, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Reopen when recording

## Trigger

The User tries to record something in a closed account and is told it is closed.

## Outcome

The account is open again.
