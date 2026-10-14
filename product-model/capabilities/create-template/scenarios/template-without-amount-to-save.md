---
kind: validation
routes:
  app: Android app
steps:
- text: The User saves a template with no amount
  kind: actor
  actor: user
  entities:
  - {entity: template, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::template-editor
- text: The Product says a template without amount should have the default action Apply and edit, and a plan without amount should not execute automatically
  kind: condition
  entities:
  - {entity: template, effect: reads, facts: [Amount, Default action, Automatic execution]}
  contexts:
    app:
      place: android-app::template-editor
---

# No amount

## Trigger

The User saves a template without amount whose default action is Apply and save, or an automatic plan without amount.

## Outcome

Nothing is saved.
