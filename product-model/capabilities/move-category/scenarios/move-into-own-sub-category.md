---
kind: validation
routes:
  app: Android app
steps:
- text: The User picks one of the category's own sub categories as new parent
  kind: actor
  actor: user
  entities:
  - {entity: category, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::categories
- text: The Product refuses the move because it would make the category hierarchy inconsistent
  kind: condition
  entities:
  - {entity: category, effect: reads, facts: [Parent category]}
  contexts:
    app:
      place: android-app::categories
---

# Move into itself

## Trigger

The User picks one of the category's own sub categories as the new parent.

## Outcome

The category stays where it was.
