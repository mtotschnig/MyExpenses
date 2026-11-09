---
kind: validation
routes:
  app: Android app
steps:
- text: The User opens Budgeting
  kind: actor
  actor: user
  entities:
  - {entity: budget, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
- text: The installation has no Professional licence and the budget trial has ended
  kind: condition
  entities:
  - {entity: licence, effect: reads, facts: [Licence, Budget trial]}
  - {entity: budget, effect: reads, facts: []}
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

The User opens Budgeting without a Professional licence after the trial has ended.

## Outcome

Budgeting does not open; a licence is offered.
