---
kind: edge
routes:
  app: Android app
steps:
- text: The Product finds it cannot read the planner calendar
  kind: condition
  unattended: true
  entities:
  - {entity: template, effect: reads, facts: []}
  contexts:
    app:
      place: android-app
- text: The Product posts a notification explaining why plans cannot be executed
  kind: product
  entities:
  - {entity: template, effect: reads, facts: []}
  contexts:
    app:
      place: android-app
---

# No calendar access

## Trigger

The planner runs while the app has no calendar access or no planner calendar.

## Outcome

No occurrence is executed; an error notification is posted.
