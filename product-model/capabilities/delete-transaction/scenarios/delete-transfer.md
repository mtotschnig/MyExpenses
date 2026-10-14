---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses Delete on a transfer and confirms
  kind: actor
  actor: user
  entities:
  - {entity: transfer, effect: reads, facts: [Amount]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product deletes both sides of the transfer
  kind: product
  actor: user
  entities:
  - {entity: transfer, effect: removes}
  contexts:
    app:
      place: android-app::transactions
---

# Delete a transfer

## Trigger

The User deletes one side of a transfer.

## Outcome

Both sides are gone from both accounts.

## Edge cases

- When the other side is a part of a split, that part stays in the split as an ordinary part.
