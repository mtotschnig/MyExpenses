---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Unpack on an archive and confirms
  kind: actor
  actor: user
  entities:
  - {entity: archive, effect: reads, facts: [Period]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product puts the archived transactions back into the list and removes the archive
  kind: product
  actor: user
  entities:
  - {entity: archive, effect: removes}
  - {entity: transaction, effect: changes, facts: [Archive]}
  - {entity: split-transaction, effect: changes, facts: [Archive]}
  contexts:
    app:
      place: android-app::transactions
---

# Unpack an archive

## Trigger

The User chooses Unpack on an archive and confirms.

## Outcome

The archived transactions are back in the list and the archive is gone.
