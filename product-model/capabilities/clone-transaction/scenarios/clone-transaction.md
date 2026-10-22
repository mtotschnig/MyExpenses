---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Clone on a transaction
  kind: actor
  actor: user
  entities:
  - {entity: transaction, as: original, effect: reads, facts: [Amount]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product opens a new transaction filled with the same details
  kind: product
  actor: user
  entities:
  - {entity: transaction, as: original, effect: reads, facts: [Account, Amount, Date, Value date, Category, Party, Payment method, Reference number, Notes, Tags, Equivalent amount]}
  contexts:
    app:
      place: android-app::transaction-editor
- text: The User adjusts the copy and saves
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::transaction-editor
- text: The Product records the copy as unreconciled
  kind: product
  actor: user
  entities:
  - {entity: transaction, as: copy, effect: creates, to: Unreconciled, facts: [Account, Amount, Date, Value date, Category, Party, Payment method, Reference number, Notes, Tags, Equivalent amount]}
  contexts:
    app:
      place: android-app::transaction-editor
---

# Clone a transaction

## Trigger

The User chooses Clone on a transaction.

## Outcome

A copy is recorded as unreconciled; the original is unchanged.

## Edge cases

- The copy is dated now unless a setting keeps the original date.
- Transfers and split transactions, with their parts, are cloned the same way.
