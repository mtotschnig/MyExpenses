---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Rollover, then Total
  kind: actor
  actor: user
  entities:
  - {entity: budget, effect: reads, facts: [Available]}
  contexts:
    app:
      place: android-app::budget
- text: The Product carries what is left of the whole budget into the next period
  kind: product
  actor: user
  entities:
  - {entity: budget, effect: changes, facts: [Rollovers]}
  contexts:
    app:
      place: android-app::budget
---

# Roll over the total

## Trigger

The User chooses Rollover, then Total, on a period of a repeating budget.

## Outcome

What is left of the budget is available in the next period.
