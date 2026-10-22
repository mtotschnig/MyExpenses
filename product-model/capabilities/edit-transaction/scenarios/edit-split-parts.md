---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses Edit on a split transaction
  kind: actor
  actor: user
  entities:
  - {entity: split-transaction, effect: reads, facts: [Amount]}
  contexts:
    app:
      place: android-app::transactions
- text: The User adds, changes or deletes parts and saves once nothing is left unsplit
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::transaction-editor
- text: The Product saves the split transaction and its parts
  kind: product
  actor: user
  entities:
  - {entity: split-transaction, effect: changes, facts: [Amount, Date, Party, Payment method, Notes, Tags, Parts]}
  contexts:
    app:
      place: android-app::transaction-editor
---

# Change a split

## Trigger

The User chooses Edit on a split transaction.

## Outcome

The split keeps its new parts.

## Edge cases

- A split cannot move to an account that one of its transfer parts goes to.
