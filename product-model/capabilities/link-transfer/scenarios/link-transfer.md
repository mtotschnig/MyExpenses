---
kind: primary
routes:
  app: Android app
steps:
- text: The User selects two transactions of different accounts, chooses New Transfer and confirms
  kind: actor
  actor: user
  entities:
  - {entity: transaction, as: outgoing, effect: reads, facts: [Amount]}
  - {entity: transaction, as: incoming, effect: reads, facts: [Amount]}
  - {entity: transfer, effect: reads, facts: []}
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
- text: The Product links them into one transfer, dropping their party and category
  kind: product
  actor: user
  entities:
  - {entity: transaction, as: outgoing, effect: removes, from: Unreconciled}
  - {entity: transaction, as: incoming, effect: removes, from: Unreconciled}
  - {entity: transfer, effect: creates, facts: [From account, To account, Amount, Received amount, Date, Notes, From status, To status]}
  - {entity: category, effect: reads, facts: []}
  - {entity: party, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Link two transactions

## Trigger

The User selects a transaction in one account and its counterpart in another and chooses New Transfer, confirming that counterparty and category are dropped.

## Outcome

The two transactions are the two sides of one transfer.

## Edge cases

- Linking is offered only for exactly two selected entries that are neither splits, transfers nor void, in different accounts, whose amounts are opposite or whose currencies differ.
