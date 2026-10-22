---
kind: validation
routes:
  app: Android app
steps:
- text: The User chooses CSV import
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::settings
- text: The installation has no Extended licence and the CSV import trial has ended
  kind: condition
  entities:
  - {entity: licence, effect: reads, facts: [Licence, CSV import trial]}
  contexts:
    app:
      place: android-app::settings
- text: The Product says the trial has ended and offers a licence
  kind: product
  actor: user
  entities:
  - {entity: licence, effect: reads, facts: [Licence]}
  contexts:
    app:
      place: android-app::settings
---

# Trial over

## Trigger

The User opens CSV import without an Extended licence after the trial has ended.

## Outcome

CSV import does not open; a licence is offered.
