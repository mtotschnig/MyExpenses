---
kind: alternative
routes:
  app: Android app
steps:
- text: The User selects open occurrences and chooses Apply and save
  kind: actor
  actor: user
  entities:
  - {entity: plan-instance, effect: reads, facts: [Date]}
  contexts:
    app:
      place: android-app::planner
- text: The Product records a transaction for each selected occurrence that is still open and marks it applied
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: creates, to: Unreconciled, facts: [Account, Amount, Date, Category, Party, Payment method, Notes, Tags]}
  - {entity: plan-instance, effect: changes, from: Open, to: Applied, facts: [Transaction]}
  contexts:
    app:
      place: android-app::planner
---

# Apply several occurrences

## Trigger

The User selects several open occurrences in the planner and chooses Apply and save.

## Outcome

Each selected occurrence is applied with its own transaction.
