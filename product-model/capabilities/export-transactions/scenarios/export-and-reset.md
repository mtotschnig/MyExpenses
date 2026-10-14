---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses Export and reset, ticks deletion and chooses how the balance is kept
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::export
- text: The Product writes the file
  kind: product
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name, Currency]}
  - {entity: transaction, effect: reads, facts: [Amount]}
  contexts:
    app:
      place: android-app::export
- text: The Product deletes the exported transactions and split transactions and keeps the balance as chosen
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: removes, from: Unreconciled}
  - {entity: split-transaction, effect: removes, from: Unreconciled}
  - {entity: account, effect: changes, facts: [Opening balance]}
  contexts:
    app:
      place: android-app::export
---

# Export and delete

## Trigger

The User chooses Export and reset and ticks deleting the exported transactions.

## Outcome

The exported transactions are gone and the account's balance is unchanged.

## Decision points

### Keeping the balance

How is the balance of the deleted transactions kept?

- Update opening balance → the account's opening balance is raised by their sum.
- Create helper transaction → one transaction for their sum, with the file name as its notes, stays in the account.

## Edge cases

- With a filter set, only the matching transactions are deleted.
