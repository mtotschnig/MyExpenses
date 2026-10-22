---
kind: primary
routes:
  app: Android app
steps:
- text: The User selects templates, chooses Delete and confirms
  kind: actor
  actor: user
  entities:
  - {entity: template, effect: reads, facts: [Title]}
  contexts:
    app:
      place: android-app::templates
- text: The Product deletes the templates with their plans and the record of their occurrences; entries already recorded stay
  kind: product
  actor: user
  entities:
  - {entity: template, effect: removes}
  - {entity: plan-instance, effect: removes, from: Applied, with: template}
  contexts:
    app:
      place: android-app::templates
---

# Delete templates

## Trigger

The User selects templates, chooses Delete and confirms.

## Outcome

The templates and their plans are gone; what they recorded stays.

## Edge cases

- The plan's calendar event is deleted only while the app has calendar access.
