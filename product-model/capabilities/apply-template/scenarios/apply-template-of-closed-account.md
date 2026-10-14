---
kind: validation
routes:
  app: Android app
steps:
- text: The User taps a locked template
  kind: actor
  actor: user
  entities:
  - {entity: template, effect: reads, facts: [Title]}
  contexts:
    app:
      place: android-app::templates
- text: The Product says the template refers to a closed account
  kind: condition
  entities:
  - {entity: account, effect: reads, facts: []}
  - {entity: template, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::templates
---

# Closed account

## Trigger

The User taps a template whose account is closed.

## Outcome

Nothing is recorded.
