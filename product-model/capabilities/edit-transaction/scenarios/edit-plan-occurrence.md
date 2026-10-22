---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses Edit single plan occurrence on an applied occurrence
  kind: actor
  actor: user
  entities:
  - {entity: plan-instance, effect: reads, facts: [Date, Transaction]}
  contexts:
    app:
      place: android-app::planner
- text: The User changes the recorded transaction and saves
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Amount]}
  contexts:
    app:
      place: android-app::transaction-editor
- text: The Product saves the transaction
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: changes, facts: [Amount, Date, Category, Party, Notes, Tags]}
  contexts:
    app:
      place: android-app::transaction-editor
---

# Change what a plan recorded

## Trigger

The User chooses Edit single plan occurrence on an applied occurrence.

## Outcome

The recorded transaction keeps the changes and the occurrence stays applied.
