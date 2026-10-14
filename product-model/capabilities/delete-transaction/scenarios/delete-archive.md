---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses Delete on an archive and confirms
  kind: actor
  actor: user
  entities:
  - {entity: archive, effect: reads, facts: [Transaction count]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product deletes the archive with the transactions it holds
  kind: product
  actor: user
  entities:
  - {entity: archive, effect: removes}
  - {entity: transaction, effect: removes, from: Reconciled, with: archive}
  - {entity: split-transaction, effect: removes, from: Reconciled, with: archive}
  contexts:
    app:
      place: android-app::transactions
---

# Delete an archive

## Trigger

The User chooses Delete on an archive and confirms that its transactions are deleted.

## Outcome

The archive and every transaction it held are gone.
