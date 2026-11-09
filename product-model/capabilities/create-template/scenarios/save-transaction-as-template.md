---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses to keep a recorded entry for reuse
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Amount]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product opens the template form filled with the transaction's details
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Account, Amount, Category, Party, Payment method, Notes, Tags]}
  - {entity: template, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::template-editor
- text: The User gives a title and saves
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::template-editor
- text: The Product saves the template
  kind: product
  actor: user
  entities:
  - {entity: template, effect: creates, facts: [Title, Account, Kind, Amount, Category, Party, Payment method, Notes, Tags, To account, Parts, Default action]}
  contexts:
    app:
      place: android-app::template-editor
---

# Save a transaction as template

## Trigger

The User chooses Save as template on a recorded transaction.

## Outcome

A template with the transaction's details is listed.
