---
kind: alternative
routes:
  app: Android app
steps:
- text: The User selects transactions, chooses Remap, picks another account and confirms
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Account]}
  - {entity: account, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product moves the transactions to the chosen account
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: changes, facts: [Account]}
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Move to another account

## Trigger

The User remaps selected transactions to another account.

## Outcome

The transactions are recorded in the other account; split parts move with their split.
