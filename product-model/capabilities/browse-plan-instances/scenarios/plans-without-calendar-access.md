---
kind: validation
routes:
  app: Android app
steps:
- text: The User taps a template with a plan
  kind: actor
  actor: user
  entities:
  - {entity: template, effect: reads, facts: [Title]}
  contexts:
    app:
      place: android-app::templates
- text: The Product asks for calendar access, which plans need
  kind: condition
  entities:
  - {entity: template, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::templates
---

# No calendar access

## Trigger

The User taps a template with a plan while the app has no calendar access.

## Outcome

Calendar access is requested; no occurrences are shown.
