---
kind: alternative
routes:
  app: Android app
steps:
- text: The User taps the budgeted amount and enters a new one
  kind: actor
  actor: user
  entities:
  - {entity: budget, effect: reads, facts: [Budgeted amount]}
  contexts:
    app:
      place: android-app::budget
- text: The Product saves the budgeted amount
  kind: product
  actor: user
  entities:
  - {entity: budget, effect: changes, facts: [Budgeted amount]}
  contexts:
    app:
      place: android-app::budget
---

# Change the budgeted amount

## Trigger

The User taps the budgeted amount of the shown period.

## Outcome

The budget has the new total.
