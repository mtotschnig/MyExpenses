---
kind: validation
routes:
  app: Android app
steps:
- text: The User chooses Close on an account linked to a synchronization backend
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::accounts
- text: The Product says that an account linked to a synchronization backend cannot be closed
  kind: condition
  entities:
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::accounts
---

# Synchronized account stays open

## Trigger

The User chooses Close on an account linked to a synchronization backend.

## Outcome

The account stays open.
