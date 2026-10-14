---
kind: primary
result: achieved
routes:
  app: Android app
steps:
- text: The User chooses New Transfer while only one account exists
  kind: actor
  actor: user
  capability: create-transaction
  entities:
  - {entity: transfer, effect: reads, facts: []}
  - {entity: account, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product says that a transfer requires a second account and offers New account
  kind: product
  actor: user
  capability: create-transaction
  entities:
  - {entity: account, effect: reads, facts: []}
  - {entity: transfer, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
- text: The User chooses New account and enters the new account
  kind: actor
  actor: user
  capability: create-account
  entities:
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::account-editor
- text: The Product creates the open account
  kind: product
  actor: user
  capability: create-account
  entities:
  - {entity: account, effect: creates, to: Open, facts: [Name, Description, Currency, Account type, Opening balance, Color]}
  contexts:
    app:
      place: android-app::account-editor
- text: The Product opens a new transfer
  kind: product
  actor: user
  capability: create-transaction
  entities:
  - {entity: transfer, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transaction-editor
- text: The User enters the amount and saves
  kind: actor
  actor: user
  capability: create-transaction
  entities: []
  contexts:
    app:
      place: android-app::transaction-editor
- text: The Product records the transfer with one side in each account
  kind: product
  actor: user
  capability: create-transaction
  entities:
  - {entity: transfer, effect: creates, facts: [From account, To account, Amount, Received amount, Date, Value date, Category, Notes, Tags, Equivalent amount, From status, To status]}
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transaction-editor
---

# Create the account, then transfer

## Trigger

The User wants to move money while only one account exists.

## Outcome

The transfer is recorded between the existing account and the new one.
