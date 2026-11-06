---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Apply and save on an open occurrence
  kind: actor
  actor: user
  entities:
  - {entity: plan-instance, effect: reads, facts: [Date]}
  contexts:
    app:
      place: android-app::planner
- text: The Product records the template's transaction on the occurrence's date and marks the occurrence applied
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: creates, to: Unreconciled, facts: [Account, Amount, Date, Category, Party, Payment method, Notes, Tags]}
  - {entity: plan-instance, effect: changes, from: Open, to: Applied, facts: [Transaction]}
  - {entity: template, effect: reads, facts: [Account, Amount]}
  contexts:
    app:
      place: android-app::planner
---

# Apply an occurrence

## Trigger

The User chooses Apply and save on an open occurrence.

## Outcome

The transaction is recorded with the occurrence's date and the occurrence is applied.
