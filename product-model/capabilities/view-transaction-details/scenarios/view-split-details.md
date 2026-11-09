---
kind: alternative
routes:
  app: Android app
steps:
- text: The User taps a split transaction
  kind: actor
  actor: user
  entities:
  - {entity: split-transaction, effect: reads, facts: [Amount]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product shows its details with each part
  kind: product
  actor: user
  entities:
  - {entity: split-transaction, effect: reads, facts: [Account, Amount, Date, Party, Parts, Notes, Exported]}
  contexts:
    app:
      place: android-app::transaction-details
---

# Open a split

## Trigger

The User taps a split transaction.

## Outcome

The split and each of its parts are shown.

## Edge cases

- Tapping an archive lists the transactions it holds.
