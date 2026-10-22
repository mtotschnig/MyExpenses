---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Archive and picks a date range
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name]}
  - {entity: archive, effect: reads, facts: [Period]}
  contexts:
    app:
      place: android-app::transactions
- text: The range holds at least one transaction, no archive, and, in an account that supports reconciliation, one status apart from void ones
  kind: condition
  entities:
  - {entity: transaction, effect: reads, facts: []}
  - {entity: account, effect: reads, facts: []}
  - {entity: archive, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
- text: The User confirms
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::transactions
- text: The Product creates an archive for the range and packs the transactions into it
  kind: product
  actor: user
  entities:
  - {entity: archive, effect: creates, facts: [Period, Amount, Date, Status, Transaction count]}
  - {entity: transaction, effect: changes, facts: [Archive]}
  - {entity: split-transaction, effect: changes, facts: [Archive]}
  contexts:
    app:
      place: android-app::transactions
---

# Archive a date range

## Trigger

The User chooses Archive on an open account and picks a date range whose transactions share one status.

## Outcome

One archive stands for the range in the list; the balance is unchanged.

## Edge cases

- The archive's amount leaves out void transactions and its date is that of the latest transaction.
- In an account without reconciliation, mixed statuses give an unreconciled archive.
