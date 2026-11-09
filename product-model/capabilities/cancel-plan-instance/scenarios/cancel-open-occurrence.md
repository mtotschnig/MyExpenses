---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Cancel on an open occurrence
  kind: actor
  actor: user
  entities:
  - {entity: plan-instance, effect: reads, facts: [Date]}
  contexts:
    app:
      place: android-app::plan-calendar
- text: The Product marks the occurrence cancelled
  kind: product
  actor: user
  entities:
  - {entity: plan-instance, effect: changes, from: Open, to: Cancelled, facts: []}
  contexts:
    app:
      place: android-app::plan-calendar
---

# Skip an occurrence

## Trigger

The User chooses Cancel on an open occurrence.

## Outcome

The occurrence is cancelled and will not be executed.
