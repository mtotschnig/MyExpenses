---
kind: validation
routes:
  app: Android app
steps:
- text: The User selects a category that is in use and chooses Delete
  kind: actor
  actor: user
  entities:
  - {entity: category, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::categories
- text: The Product keeps it and reports how many categories are used by transactions or templates
  kind: condition
  entities:
  - {entity: transaction, effect: reads, facts: []}
  - {entity: template, effect: reads, facts: []}
  - {entity: category, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::categories
---

# Category in use

## Trigger

The User deletes a category that transactions or templates use.

## Outcome

The category stays; the result says how many could not be deleted and why.
