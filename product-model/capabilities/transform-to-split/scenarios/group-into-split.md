---
kind: primary
routes:
  app: Android app
steps:
- text: The User selects transactions, chooses Transform to split and confirms
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Amount, Date]}
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
- text: The Product turns the transactions into the parts of one new split transaction carrying their common party and status, their summed amount and their average date
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: removes, from: Unreconciled}
  - {entity: split-transaction, effect: creates, to: Unreconciled, facts: [Account, Amount, Date, Value date, Party, Parts, Equivalent amount]}
  - {entity: party, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
---

# Group transactions into a split

## Trigger

The User selects several transactions and chooses Transform to split, confirming that their dates are replaced by their average.

## Outcome

One split transaction with the average date holds the transactions as its parts.

## Edge cases

- Selecting a single transaction turns it into a split with one part.
- The parts keep their categories and amounts but lose their own party and date.
