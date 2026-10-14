---
kind: alternative
routes:
  app: Android app
steps:
- text: The User enters the transaction, turns on Save as template and optionally gives a title and a recurrence
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: []}
  - {entity: template, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transaction-editor
- text: The Product records the transaction
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: creates, to: Unreconciled, facts: [Account, Amount, Date, Value date, Category, Party, Payment method, Reference number, Notes, Tags, Equivalent amount]}
  contexts:
    app:
      place: android-app::transaction-editor
- text: The Product creates a template from it, titled after the party, category or notes when no title is given; with a recurrence the template gets a plan whose first occurrence is applied with this transaction
  kind: product
  actor: user
  entities:
  - {entity: template, effect: creates, facts: [Title, Account, Kind, Amount, Category, Party, Payment method, Notes, Tags, Recurrence, Plan start, Default action]}
  - {entity: plan-instance, effect: creates, to: Applied, facts: [Date, Transaction], with: template}
  - {entity: transaction, effect: reads, facts: []}
  - {entity: category, effect: reads, facts: []}
  - {entity: party, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transaction-editor
---

# Record and keep as template or plan

## Trigger

The User turns on Save as template while recording.

## Outcome

The transaction is recorded and a template made from it; with a recurrence the template has a plan whose first occurrence is this transaction.
