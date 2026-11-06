---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Edit on a transaction
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Amount]}
  contexts:
    app:
      place: android-app::transactions
- text: The User changes its details and saves
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::transaction-editor
- text: The Product saves the changes
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: changes, facts: [Account, Amount, Date, Value date, Category, Party, Payment method, Reference number, Notes, Tags, Equivalent amount, Original amount]}
  contexts:
    app:
      place: android-app::transaction-editor
---

# Change a transaction

## Trigger

The User chooses Edit on a transaction.

## Outcome

The transaction keeps the changes and balances follow.

## Edge cases

- Changing the account moves the transaction to that account.
- The status can be set to unreconciled, cleared or void in accounts that support reconciliation, and is not offered once reconciled.
