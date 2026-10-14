---
kind: alternative
routes:
  app: Android app
steps:
- text: The User maps the columns and chooses a new account with its currency and type as target
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::csv-import
- text: The Product creates the account and records the rows in it
  kind: product
  actor: user
  entities:
  - {entity: account, effect: creates, to: Open, facts: [Name, Currency, Account type, Opening balance]}
  - {entity: transaction, effect: creates, to: Unreconciled, facts: [Account, Amount, Date, Category, Party, Payment method, Reference number, Notes], with: account}
  contexts:
    app:
      place: android-app::csv-import
---

# Import into a new account

## Trigger

The User chooses a new account as target.

## Outcome

A new account named after the CSV import holds the rows.
