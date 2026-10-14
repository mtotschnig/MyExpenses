---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses Mark as cleared on a transfer
  kind: actor
  actor: user
  entities:
  - {entity: transfer, effect: reads, facts: [Amount, From status]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product marks the side of the transfer in this account cleared
  kind: product
  actor: user
  entities:
  - {entity: transfer, effect: changes, facts: [From status]}
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Mark one side of a transfer as cleared

## Trigger

The User taps the status of a transfer in one of its accounts.

## Outcome

Only the side in that account is cleared.
