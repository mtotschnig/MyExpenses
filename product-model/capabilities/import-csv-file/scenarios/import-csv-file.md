---
kind: primary
routes:
  app: Android app
steps:
- text: The User picks a CSV file, its delimiter and encoding
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::csv-import
- text: The User maps columns to fields, sets the date format, selects the rows and the target account
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::csv-import
- text: Without an Extended licence, the Product starts the 60-day trial on first use
  kind: product
  actor: user
  entities:
  - {entity: licence, effect: changes, facts: [CSV import trial]}
  contexts:
    app:
      place: android-app::csv-import
- text: The Product adds the parties and categories found in the rows
  kind: product
  actor: user
  entities:
  - {entity: party, effect: creates, facts: [Name]}
  - {entity: category, effect: creates, facts: [Name, Parent category]}
  contexts:
    app:
      place: android-app::csv-import
- text: The Product records the selected rows as transactions
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: creates, to: Unreconciled, facts: [Account, Amount, Date, Category, Party, Payment method, Reference number, Notes, Tags]}
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::csv-import
---

# Import a CSV file

## Trigger

The User picks a CSV file in CSV import.

## Outcome

The selected rows are transactions in the target account, with a summary of what was imported and discarded.

## Edge cases

- With an account column, rows go to the open accounts of those names, created when missing.
- Without a licence, each use during the trial first shows how long the trial lasts and offers the licence.
- Rows that cannot be parsed are discarded and counted in the summary.
