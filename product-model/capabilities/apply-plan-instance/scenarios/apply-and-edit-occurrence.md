---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses Apply and edit on an open occurrence
  kind: actor
  actor: user
  entities:
  - {entity: plan-instance, effect: reads, facts: [Date]}
  contexts:
    app:
      place: android-app::plan-calendar
- text: The Product opens the template's transaction dated on the occurrence
  kind: product
  actor: user
  entities:
  - {entity: template, effect: reads, facts: [Amount]}
  - {entity: transaction, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transaction-editor
- text: The User adjusts and saves it
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Amount]}
  contexts:
    app:
      place: android-app::transaction-editor
- text: The Product records the transaction and marks the occurrence applied
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: creates, to: Unreconciled, facts: [Account, Amount, Date, Category, Party, Payment method, Notes, Tags]}
  - {entity: plan-instance, effect: changes, from: Open, to: Applied, facts: [Transaction]}
  contexts:
    app:
      place: android-app::transaction-editor
---

# Apply and edit an occurrence

## Trigger

The User chooses Apply and edit on an open occurrence in the plan calendar.

## Outcome

The edited transaction is recorded and the occurrence is applied.
