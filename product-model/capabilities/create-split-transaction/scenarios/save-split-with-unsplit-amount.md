---
kind: validation
routes:
  app: Android app
steps:
- text: The User saves while part of the total is not yet split
  kind: actor
  actor: user
  entities:
  - {entity: split-transaction, effect: reads, facts: [Amount, Parts]}
  contexts:
    app:
      place: android-app::transaction-editor
- text: The Product says that some amount is still left unsplit
  kind: condition
  entities:
  - {entity: split-transaction, effect: reads, facts: [Amount, Parts]}
  contexts:
    app:
      place: android-app::transaction-editor
---

# Amount left unsplit

## Trigger

The User saves a split whose parts do not add up to the total.

## Outcome

Nothing is recorded until the parts add up.
