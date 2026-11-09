---
kind: primary
routes:
  app: Android app
steps:
- text: The User opens a budget
  kind: actor
  actor: user
  entities:
  - {entity: budget, effect: reads, facts: [Title]}
  contexts:
    app:
      place: android-app::budgets
- text: The Product shows the budgeted amount and, per category, the allocation, rollover, spending and what is available for the current period
  kind: product
  actor: user
  entities:
  - {entity: budget, effect: reads, facts: [Period, Filters, Budgeted amount, Category allocations, Rollovers, Spent, Available]}
  - {entity: category, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::budget
---

# Follow a budget

## Trigger

The User opens a budget.

## Outcome

The current period's allocations, rollovers, spending and availability are shown per category.

## Edge cases

- The User moves to earlier and later periods.
- The list can be narrowed to categories with an allocation, and neutral categories aggregated.
