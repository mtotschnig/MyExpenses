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
- text: The Product names the different statuses found and does not offer to archive
  kind: condition
  entities:
  - {entity: transaction, effect: reads, facts: []}
  - {entity: archive, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Statuses differ

## Trigger

The chosen range in an account that supports reconciliation holds transactions with different statuses.

## Outcome

No archive is created.
