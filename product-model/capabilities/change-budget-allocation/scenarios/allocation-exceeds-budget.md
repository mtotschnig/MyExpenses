---
kind: validation
routes:
  app: Android app
steps:
- text: The User taps the allocation of a category
  kind: actor
  actor: user
  entities:
  - {entity: budget, effect: reads, facts: [Category allocations]}
  - {entity: category, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::budget
- text: The Product says all the budget has been allocated to other categories and suggests increasing the budgeted amount or removing money from other categories
  kind: condition
  entities:
  - {entity: budget, effect: reads, facts: [Budgeted amount, Category allocations]}
  - {entity: category, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::budget
---

# Nothing left to allocate

## Trigger

The User taps the allocation of a category when the whole budgeted amount is already allocated to other categories.

## Outcome

Nothing changes.
