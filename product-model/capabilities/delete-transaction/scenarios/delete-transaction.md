---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Delete on a transaction and confirms
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Amount]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product deletes the transaction
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: removes, from: Unreconciled}
  contexts:
    app:
      place: android-app::transactions
---

# Delete a transaction

## Trigger

The User chooses Delete on a transaction and confirms.

## Outcome

The transaction is gone and balances no longer count it.

## Edge cases

- Several selected transactions are deleted at once, with progress shown.
- Deleting a reconciled transaction warns that the records will differ from the bank statement.
