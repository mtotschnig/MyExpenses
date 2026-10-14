---
kind: primary
routes:
  app: Android app
steps:
- text: The User picks a QIF file, its date format and encoding, the currency, what to import and to import into new accounts
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::qif-import
- text: The Product adds the categories and parties found in the file
  kind: product
  actor: user
  entities:
  - {entity: category, effect: creates, facts: [Name, Parent category]}
  - {entity: party, effect: creates, facts: [Name]}
  contexts:
    app:
      place: android-app::qif-import
- text: For each account in the file, the Product uses the open account of the same name or creates one
  kind: product
  actor: user
  entities:
  - {entity: account, effect: creates, to: Open, facts: [Name, Currency, Account type, Opening balance]}
  contexts:
    app:
      place: android-app::qif-import
- text: The Product records the file's transactions and transfers and reports how many were imported per account
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: creates, to: Unreconciled, facts: [Account, Amount, Date, Category, Party, Payment method, Reference number, Notes]}
  - {entity: transfer, effect: creates, facts: [From account, To account, Amount, Received amount, Date]}
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::qif-import
---

# Import a QIF file

## Trigger

The User picks a QIF file in QIF import.

## Outcome

The file's categories, parties, accounts and transactions are in the app, with a summary of what was imported.

## Edge cases

- Cleared, reconciled and void markers in the file set the status.
- Categories missing in the file can be filled in from the party's last transaction.
