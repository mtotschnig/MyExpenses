---
kind: validation
routes:
  app: Android app
steps:
- text: The User picks a file holding several accounts and an existing account as target
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::qif-import
- text: The Product says the file holds several accounts that cannot be merged into one
  kind: condition
  entities:
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::qif-import
---

# Several accounts into one

## Trigger

The User imports a file holding several accounts into one existing account.

## Outcome

No transactions are imported.
