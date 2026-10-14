---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses Rollover, then Edit, enters amounts per category and saves
  kind: actor
  actor: user
  entities:
  - {entity: budget, effect: reads, facts: [Available, Rollovers]}
  - {entity: category, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::budget
- text: The Product carries the entered amounts into the next period
  kind: product
  actor: user
  entities:
  - {entity: budget, effect: changes, facts: [Rollovers]}
  contexts:
    app:
      place: android-app::budget
---

# Choose the rollover amounts

## Trigger

The User chooses Rollover, then Edit, sets amounts per category and saves.

## Outcome

The chosen amounts are carried into the next period.

## Edge cases

- A category's rollover together with its sub categories' rollovers cannot exceed what is left.
