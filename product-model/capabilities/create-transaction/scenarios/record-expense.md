---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses New Transaction
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
- text: The User enters the amount, category, party and date, and optionally payment method, reference number, notes and tags
  kind: actor
  actor: user
  entities:
  - {entity: category, effect: reads, facts: []}
  - {entity: party, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transaction-editor
- text: The Product adds the party when its name is new
  kind: product
  actor: user
  entities:
  - {entity: party, effect: creates, facts: [Name]}
  contexts:
    app:
      place: android-app::transaction-editor
- text: The Product records the transaction as unreconciled
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: creates, to: Unreconciled, facts: [Account, Amount, Date, Value date, Category, Party, Payment method, Reference number, Notes, Tags, Equivalent amount]}
  contexts:
    app:
      place: android-app::transaction-editor
- text: The Product returns to the list, whose balance includes the new transaction
  kind: product
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Current balance]}
  - {entity: transaction, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Record an expense

## Trigger

The User chooses New Transaction.

## Outcome

The expense is in the account's list and its balance.

## Edge cases

- Switching the amount to income records an income with the same form.
- With Save and new on, the form stays open for the next transaction with the same data.
- Choosing a known party fills in the category, amount, notes and payment method of its last transaction, as the settings say.
- An amount paid in another currency is entered as the original amount, and an account in a foreign currency also takes the equivalent amount.
- Choosing an income category for an expense, or the reverse, asks for confirmation.
- New transactions get the account's active tags.
