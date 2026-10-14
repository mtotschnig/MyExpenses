---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses Apply and edit on a template
  kind: actor
  actor: user
  entities:
  - {entity: template, effect: reads, facts: [Title]}
  contexts:
    app:
      place: android-app::templates
- text: The Product opens a new transaction filled from the template
  kind: product
  actor: user
  entities:
  - {entity: template, effect: reads, facts: [Account, Amount, Category, Party, Payment method, Notes, Tags]}
  - {entity: transaction, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transaction-editor
- text: The User adjusts the transaction and saves
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Amount]}
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

# Apply and edit

## Trigger

The User taps a template whose default action is Apply and edit, or chooses Apply and edit.

## Outcome

The transaction is recorded with the changes made in the form.
