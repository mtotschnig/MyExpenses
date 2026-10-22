---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Export
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::transactions
- text: The User picks QIF, CSV or JSON, the file name, date format, decimal separator and encoding, and whether to include only what was not exported yet
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::export
- text: The Product writes the account's transactions, transfers and split transactions that match the current filter to the file and marks them exported
  kind: product
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name, Currency]}
  - {entity: transaction, effect: changes, facts: [Exported]}
  - {entity: transfer, effect: changes, facts: [Exported]}
  - {entity: split-transaction, effect: changes, facts: [Exported]}
  contexts:
    app:
      place: android-app::export
- text: The Product reports the file written and offers to share it
  kind: product
  actor: user
  entities:
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::export
---

# Export to a file

## Trigger

The User chooses Export on an account.

## Outcome

A file with the account's transactions matching the current filter is written and offered for sharing; the transactions are marked exported.
