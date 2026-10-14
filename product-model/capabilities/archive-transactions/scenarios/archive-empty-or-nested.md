---
kind: validation
routes:
  app: Android app
steps:
- text: The User picks a date range
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name]}
  - {entity: archive, effect: reads, facts: [Period]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product says that no transactions exist in the range, or that nested archives are not supported
  kind: condition
  entities:
  - {entity: transaction, effect: reads, facts: []}
  - {entity: archive, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Empty range or nested archive

## Trigger

The chosen range holds no transaction, or already holds an archive.

## Outcome

No archive is created.
