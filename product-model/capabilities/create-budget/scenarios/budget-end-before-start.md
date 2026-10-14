---
kind: validation
routes:
  app: Android app
steps:
- text: The User saves a custom range ending before it starts
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::budget-editor
- text: The Product says the end date must come after the start date
  kind: condition
  entities:
  - {entity: budget, effect: reads, facts: [Start date, End date]}
  contexts:
    app:
      place: android-app::budget-editor
---

# End before start

## Trigger

The User saves a custom range whose end comes before its start.

## Outcome

Nothing is saved.
