---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses Balance account, ticks Delete reconciled transactions and confirms
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name]}
  - {entity: transaction, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
- text: The Product marks every cleared transaction and split transaction of the account reconciled
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: changes, from: Cleared, to: Reconciled, facts: []}
  - {entity: split-transaction, effect: changes, from: Cleared, to: Reconciled, facts: []}
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
- text: The Product deletes the reconciled transactions and split transactions and raises the opening balance of the account by their sum
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: removes, from: Reconciled}
  - {entity: split-transaction, effect: removes, from: Reconciled}
  - {entity: account, effect: changes, facts: [Opening balance]}
  contexts:
    app:
      place: android-app::transactions
---

# Balance and clean up

## Trigger

The User ticks Delete reconciled transactions in the balance dialog.

## Outcome

Reconciled entries are gone; the opening balance has absorbed them, so the current balance is unchanged.

## Edge cases

- The other side of a deleted transfer stays in its account as an ordinary transaction.
