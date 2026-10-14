---
kind: validation
routes:
  app: Android app
steps:
- text: The User imports a file into new accounts
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::qif-import
- text: The new accounts would exceed five and the installation has no Contrib licence
  kind: condition
  entities:
  - {entity: account, effect: reads, facts: []}
  - {entity: licence, effect: reads, facts: [Licence]}
  contexts:
    app:
      place: android-app::qif-import
- text: The Product stops and explains the limit of the current licence
  kind: product
  actor: user
  entities:
  - {entity: licence, effect: reads, facts: [Licence]}
  contexts:
    app:
      place: android-app::qif-import
---

# Too many accounts

## Trigger

Importing would create more than five accounts and there is no Contrib licence.

## Outcome

The import stops with the licence limit explained.
