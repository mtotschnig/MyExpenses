---
kind: alternative
routes:
  app: Android app
steps:
- text: The User taps Cancel on the notification of a due occurrence
  kind: actor
  actor: user
  entities:
  - {entity: plan-instance, effect: reads, facts: [Date]}
  contexts:
    app:
      place: android-app
- text: The Product marks the occurrence cancelled
  kind: product
  actor: user
  entities:
  - {entity: plan-instance, effect: changes, from: Open, to: Cancelled, facts: []}
  contexts:
    app:
      place: android-app
---

# Cancel from the notification

## Trigger

The User taps Cancel on the notification of a due occurrence.

## Outcome

The occurrence is cancelled.
