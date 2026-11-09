---
kind: validation
routes:
  app: Android app
steps:
- text: The User saves the form with an empty name
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::account-editor
- text: The Product marks the name as required and creates nothing
  kind: condition
  entities:
  - {entity: account, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::account-editor
---

# Name missing

## Trigger

The User saves the account form without a name.

## Outcome

No account is created; the name is marked as required.
