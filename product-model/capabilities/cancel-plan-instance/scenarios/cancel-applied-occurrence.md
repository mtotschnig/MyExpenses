---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses Cancel on an applied occurrence and confirms
  kind: actor
  actor: user
  entities:
  - {entity: plan-instance, effect: reads, facts: [Date, Transaction]}
  contexts:
    app:
      place: android-app::planner
- text: The Product deletes the transaction recorded for the occurrence and marks it cancelled
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: removes, from: Unreconciled}
  - {entity: plan-instance, effect: changes, from: Applied, to: Cancelled, facts: [Transaction]}
  contexts:
    app:
      place: android-app::planner
---

# Cancel an applied occurrence

## Trigger

The User chooses Cancel on an applied occurrence and confirms that its transaction is deleted.

## Outcome

The recorded transaction is gone and the occurrence is cancelled.
