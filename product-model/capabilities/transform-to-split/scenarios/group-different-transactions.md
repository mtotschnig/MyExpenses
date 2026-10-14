---
kind: validation
routes:
  app: Android app
steps:
- text: The User selects transactions that do not belong together and chooses Transform to split
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Amount]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product says only transactions that have the same account, counterparty and status can be grouped
  kind: condition
  entities:
  - {entity: transaction, effect: reads, facts: [Account, Party]}
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Transactions differ

## Trigger

The User groups transactions that differ in account, party or status.

## Outcome

Nothing changes.
