---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses Rollover, then Categories
  kind: actor
  actor: user
  entities:
  - {entity: budget, effect: reads, facts: [Available]}
  - {entity: category, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::budget
- text: The Product carries what is left of each main category into the next period
  kind: product
  actor: user
  entities:
  - {entity: budget, effect: changes, facts: [Rollovers]}
  - {entity: category, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::budget
---

# Roll over per category

## Trigger

The User chooses Rollover, then Categories.

## Outcome

Each main category's remainder is available to it in the next period.
