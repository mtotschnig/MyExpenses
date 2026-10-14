---
kind: primary
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
- text: The Product shows the reconciled total and the cleared total to compare with the statement
  kind: product
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Reconciled total, Cleared total]}
  contexts:
    app:
      place: android-app::transactions
- text: The User confirms
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::transactions
- text: The Product marks every cleared transaction, split transaction and transfer side of the account reconciled
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: changes, from: Cleared, to: Reconciled, facts: []}
  - {entity: split-transaction, effect: changes, from: Cleared, to: Reconciled, facts: []}
  - {entity: transfer, effect: changes, facts: [From status]}
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Balance against a statement

## Trigger

The User chooses Balance account after marking the transactions on the bank statement as cleared.

## Outcome

All cleared entries are reconciled and the reconciled total equals the cleared total.

## Edge cases

- Balance account is offered only for open accounts whose type supports reconciliation.
