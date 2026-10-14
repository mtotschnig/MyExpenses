---
kind: validation
routes:
  app: Android app
steps:
- text: The User saves the template form with an empty title
  kind: actor
  actor: user
  entities:
  - {entity: template, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::template-editor
- text: The Product marks the title as required
  kind: condition
  entities:
  - {entity: template, effect: reads, facts: [Title]}
  contexts:
    app:
      place: android-app::template-editor
---

# Title missing

## Trigger

The User saves a template without a title.

## Outcome

Nothing is saved; the title is marked as required.
