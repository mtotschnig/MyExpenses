---
kind: alternative
routes:
  app: Android app
steps:
- text: The User taps Apply on the notification of a due occurrence
  kind: actor
  actor: user
  entities:
  - {entity: plan-instance, effect: reads, facts: [Date]}
  contexts:
    app:
      place: android-app
- text: The Product records the transaction and marks the occurrence applied
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: creates, to: Unreconciled, facts: [Account, Amount, Date, Category, Party, Payment method, Notes, Tags]}
  - {entity: plan-instance, effect: changes, from: Open, to: Applied, facts: [Transaction]}
  contexts:
    app:
      place: android-app
---

# Apply from the notification

## Trigger

The planner notification of a due occurrence offers Apply, Edit and Cancel; the User taps Apply.

## Outcome

The occurrence is applied and the notification is updated.
