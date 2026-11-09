---
kind: alternative
routes:
  app: Android app
steps:
- text: The User picks a template from the form's Templates menu, confirming that entered data is discarded
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: []}
  - {entity: template, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transaction-editor
- text: The Product fills the form from the template
  kind: product
  actor: user
  entities:
  - {entity: template, effect: reads, facts: [Account, Amount, Category, Party, Notes, Tags]}
  contexts:
    app:
      place: android-app::transaction-editor
- text: The User saves
  kind: actor
  actor: user
  entities: []
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
---

# Load a template into the form

## Trigger

While recording, the User picks a template from the form's Templates menu.

## Outcome

The form holds the template's details, and saving records the transaction.
