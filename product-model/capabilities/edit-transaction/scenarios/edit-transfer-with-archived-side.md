---
kind: validation
routes:
  app: Android app
steps:
- text: The User chooses Edit on a transfer
  kind: actor
  actor: user
  entities:
  - {entity: transfer, effect: reads, facts: [Amount]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product says the transfer cannot be edited because its other side is archived
  kind: condition
  entities:
  - {entity: transfer, effect: reads, facts: [Archive]}
  contexts:
    app:
      place: android-app::transactions
---

# Other side archived

## Trigger

The User chooses Edit on a transfer whose other side is in an archive.

## Outcome

No form opens.
