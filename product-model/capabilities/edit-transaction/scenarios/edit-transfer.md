---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses Edit on a transfer
  kind: actor
  actor: user
  entities:
  - {entity: transfer, effect: reads, facts: [Amount]}
  contexts:
    app:
      place: android-app::transactions
- text: The User changes the accounts, amounts or details and saves
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transaction-editor
- text: The Product saves the transfer on both sides
  kind: product
  actor: user
  entities:
  - {entity: transfer, effect: changes, facts: [From account, To account, Amount, Received amount, Date, Value date, Category, Notes, Tags]}
  contexts:
    app:
      place: android-app::transaction-editor
---

# Change a transfer

## Trigger

The User chooses Edit on either side of a transfer.

## Outcome

Both sides show the changes.
