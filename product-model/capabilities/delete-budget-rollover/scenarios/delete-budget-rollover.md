---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Rollover, then Delete, and confirms
  kind: actor
  actor: user
  entities:
  - {entity: budget, effect: reads, facts: [Rollovers]}
  contexts:
    app:
      place: android-app::budget
- text: The Product deletes the rollovers from the shown period into the next
  kind: product
  actor: user
  entities:
  - {entity: budget, effect: changes, facts: [Rollovers]}
  contexts:
    app:
      place: android-app::budget
---

# Delete rollovers

## Trigger

The User chooses Rollover, then Delete, and confirms.

## Outcome

The period carries nothing into the next one.
