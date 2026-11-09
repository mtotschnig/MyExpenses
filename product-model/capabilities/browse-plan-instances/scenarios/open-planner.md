---
kind: primary
routes:
  app: Android app
steps:
- text: The User opens the planner
  kind: actor
  actor: user
  entities:
  - {entity: template, effect: reads, facts: [Title]}
  contexts:
    app:
      place: android-app::templates
- text: The Product lists the occurrences of every plan by date with their state
  kind: product
  actor: user
  entities:
  - {entity: plan-instance, effect: reads, facts: [Date, Transaction]}
  - {entity: template, effect: reads, facts: [Title, Amount, Account]}
  contexts:
    app:
      place: android-app::planner
---

# Open the planner

## Trigger

The User opens the planner from Templates and plans.

## Outcome

The upcoming occurrences of every plan are listed with their state.

## Edge cases

- Earlier and later periods are loaded on request.
