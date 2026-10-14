---
kind: primary
routes:
  app: Android app
steps:
- text: The User taps a transaction
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Amount]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product shows its details
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Account, Amount, Date, Value date, Category, Party, Payment method, Reference number, Notes, Tags, Original amount, Equivalent amount, Exported]}
  contexts:
    app:
      place: android-app::transaction-details
---

# Open the details

## Trigger

The User taps a transaction in the list.

## Outcome

All details are shown, with the actions on it.
