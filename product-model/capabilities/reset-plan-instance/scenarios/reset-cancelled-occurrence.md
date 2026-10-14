---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Reset on a cancelled occurrence
  kind: actor
  actor: user
  entities:
  - {entity: plan-instance, effect: reads, facts: [Date]}
  contexts:
    app:
      place: android-app::plan-calendar
- text: The Product makes the occurrence open again
  kind: product
  actor: user
  entities:
  - {entity: plan-instance, effect: changes, from: Cancelled, to: Open, facts: []}
  contexts:
    app:
      place: android-app::plan-calendar
---

# Reopen a skipped occurrence

## Trigger

The User chooses Reset on a cancelled occurrence.

## Outcome

The occurrence is open again.
