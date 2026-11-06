---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses New split
  kind: actor
  actor: user
  entities:
  - {entity: split-transaction, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
- text: Without a Contrib licence, the Product starts the 60-day trial on first use
  kind: product
  actor: user
  entities:
  - {entity: licence, effect: changes, facts: [Split transaction trial]}
  contexts:
    app:
      place: android-app::transactions
- text: The User enters the total, party and date and adds parts until nothing is left unsplit
  kind: actor
  actor: user
  entities:
  - {entity: split-transaction, effect: reads, facts: [Amount]}
  - {entity: party, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transaction-editor
- text: The Product records the split transaction with its parts
  kind: product
  actor: user
  entities:
  - {entity: split-transaction, effect: creates, to: Unreconciled, facts: [Account, Amount, Date, Value date, Party, Payment method, Reference number, Notes, Tags, Parts, Equivalent amount]}
  contexts:
    app:
      place: android-app::transaction-editor
---

# Record a split

## Trigger

The User chooses New split.

## Outcome

The split transaction is in the account with its parts.

## Edge cases

- A part is either a category part or a transfer part to another account, each with its own amount, notes and tags.
- Without a licence, each use during the trial first shows how long the trial lasts and offers the licence.
- Adding a part offers the amount still unsplit.
