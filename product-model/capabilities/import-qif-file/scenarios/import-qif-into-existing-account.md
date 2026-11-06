---
kind: alternative
routes:
  app: Android app
steps:
- text: The User picks a QIF file and an existing account as target
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::qif-import
- text: The Product records the file's transactions in that account
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: creates, to: Unreconciled, facts: [Account, Amount, Date, Category, Party, Payment method, Reference number, Notes]}
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::qif-import
---

# Import into an account

## Trigger

The User picks an existing account as target.

## Outcome

The file's transactions are in that account.
