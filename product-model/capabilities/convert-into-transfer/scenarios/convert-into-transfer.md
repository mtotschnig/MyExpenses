---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Convert into transfer on a transaction and picks the other account
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Amount]}
  - {entity: account, effect: reads, facts: [Name]}
  - {entity: transfer, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
- text: The Product turns the transaction into a transfer with the opposite amount in the other account; it keeps its category or takes the default transfer category, and drops the payment method
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: removes, from: Unreconciled}
  - {entity: transfer, effect: creates, facts: [From account, To account, Amount, Received amount, Date, Value date, Category, Notes]}
  - {entity: settings, effect: reads, facts: [Default transfer category]}
  - {entity: account, effect: reads, facts: []}
  - {entity: category, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Convert into a transfer

## Trigger

The User chooses Convert into transfer on a transaction and picks the other account.

## Outcome

The transaction is one side of a transfer whose other side is in the chosen account.
