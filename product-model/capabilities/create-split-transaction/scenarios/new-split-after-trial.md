---
kind: validation
routes:
  app: Android app
steps:
- text: The User chooses New split
  kind: actor
  actor: user
  entities:
  - {entity: split-transaction, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
- text: The installation has no Contrib licence and the split transaction trial has ended
  kind: condition
  entities:
  - {entity: licence, effect: reads, facts: [Licence, Split transaction trial]}
  - {entity: split-transaction, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
- text: The Product says the trial has ended and offers a licence
  kind: product
  actor: user
  entities:
  - {entity: licence, effect: reads, facts: [Licence]}
  contexts:
    app:
      place: android-app::transactions
---

# Trial over

## Trigger

The User chooses New split without a Contrib licence after the trial has ended.

## Outcome

No form opens; a licence is offered.
