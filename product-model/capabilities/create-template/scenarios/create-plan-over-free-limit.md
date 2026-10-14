---
kind: validation
routes:
  app: Android app
steps:
- text: The User chooses a recurrence
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::template-editor
- text: Three templates already have a plan and the installation has no Contrib licence
  kind: condition
  entities:
  - {entity: template, effect: reads, facts: [Recurrence]}
  - {entity: licence, effect: reads, facts: [Licence]}
  contexts:
    app:
      place: android-app::template-editor
- text: The Product says the current licence allows three plans and offers a licence
  kind: product
  actor: user
  entities:
  - {entity: licence, effect: reads, facts: [Licence]}
  contexts:
    app:
      place: android-app::template-editor
---

# Free plan limit reached

## Trigger

The User gives a template a recurrence while three plans exist and there is no Contrib licence.

## Outcome

No plan is created; the licence is offered.
