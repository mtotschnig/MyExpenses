---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses New Transfer
  kind: actor
  actor: user
  entities:
  - {entity: transfer, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
- text: The User picks the receiving account and enters the amount, and the received amount when the currencies differ
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transaction-editor
- text: The Product records the transfer with one side in each account
  kind: product
  actor: user
  entities:
  - {entity: transfer, effect: creates, facts: [From account, To account, Amount, Received amount, Date, Value date, Category, Notes, Tags, Equivalent amount, From status, To status]}
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transaction-editor
---

# Record a transfer

## Trigger

The User chooses New Transfer.

## Outcome

One side of the transfer is in each account and both balances reflect it.

## Edge cases

- Invert swaps the two accounts.
- Between currencies, the received amount and the amount set each other through the exchange rate.
