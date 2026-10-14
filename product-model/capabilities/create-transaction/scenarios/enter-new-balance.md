---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses New balance and enters the balance the bank shows
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Current balance]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product opens a new transaction whose amount is the difference to the current balance
  kind: product
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Current balance]}
  - {entity: transaction, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transaction-editor
- text: The User completes and saves the transaction
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Amount]}
  contexts:
    app:
      place: android-app::transaction-editor
- text: The Product records the transaction
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: creates, to: Unreconciled, facts: [Account, Amount, Date, Value date, Category, Party, Payment method, Reference number, Notes, Tags, Equivalent amount]}
  contexts:
    app:
      place: android-app::transaction-editor
---

# Record the difference to a new balance

## Trigger

The User chooses New balance on an account and enters the balance the bank shows.

## Outcome

A transaction for the difference is recorded and the account shows the entered balance.
