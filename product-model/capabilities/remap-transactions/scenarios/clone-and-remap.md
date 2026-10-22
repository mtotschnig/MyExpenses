---
kind: alternative
routes:
  app: Android app
steps:
- text: The User remaps selected transactions with the clone option ticked
  kind: actor
  actor: user
  entities:
  - {entity: transaction, as: original, effect: reads, facts: [Category]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product records an unreconciled copy of each, with its tags and attachments, carrying the new value
  kind: product
  actor: user
  entities:
  - {entity: transaction, as: copy, effect: creates, to: Unreconciled, facts: [Account, Amount, Date, Value date, Category, Party, Payment method, Reference number, Notes, Tags, Equivalent amount]}
  - {entity: transaction, as: original, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Remap copies

## Trigger

The User ticks the option to clone before remapping.

## Outcome

Copies with the new value are recorded; the originals are unchanged.
