---
kind: validation
routes:
  app: Android app
steps:
- text: The User enters a name used by another category at the same level
  kind: actor
  actor: user
  entities:
  - {entity: category, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::categories
- text: The Product says the name is already defined
  kind: condition
  entities:
  - {entity: category, effect: reads, facts: [Name, Parent category]}
  contexts:
    app:
      place: android-app::categories
---

# Name taken

## Trigger

The User enters a name that already exists at that level.

## Outcome

Nothing is added.
