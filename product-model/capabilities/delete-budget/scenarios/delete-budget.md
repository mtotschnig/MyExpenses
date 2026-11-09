---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Delete on a budget and confirms
  kind: actor
  actor: user
  entities:
  - {entity: budget, effect: reads, facts: [Title]}
  contexts:
    app:
      place: android-app::budget
- text: The Product deletes the budget with its allocations and rollovers
  kind: product
  actor: user
  entities:
  - {entity: budget, effect: removes}
  contexts:
    app:
      place: android-app::budget
---

# Delete a budget

## Trigger

The User chooses Delete on a budget and confirms.

## Outcome

The budget is gone.
