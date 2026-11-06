---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Unlink transfer on a transfer and confirms
  kind: actor
  actor: user
  entities:
  - {entity: transfer, effect: reads, facts: [From account, To account]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product replaces the transfer by an ordinary transaction in each of its two accounts
  kind: product
  actor: user
  entities:
  - {entity: transfer, effect: removes}
  - {entity: transaction, as: from-side, effect: creates, to: Unreconciled, facts: [Account, Amount, Date, Notes]}
  - {entity: transaction, as: to-side, effect: creates, to: Unreconciled, facts: [Account, Amount, Date, Notes]}
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Unlink a transfer

## Trigger

The User chooses Unlink transfer and confirms.

## Outcome

Each account holds an ordinary transaction instead of its side of the transfer.
