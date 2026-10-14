---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Delete on an open account and confirms
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::accounts
- text: The Product deletes the account with everything recorded in it, keeping the other side of each of its transfers as an ordinary transaction
  kind: product
  actor: user
  entities:
  - {entity: account, effect: removes, from: Open}
  - {entity: transaction, as: recorded, effect: removes, from: Unreconciled, with: account}
  - {entity: split-transaction, effect: removes, from: Unreconciled, with: account}
  - {entity: transfer, effect: removes, with: account}
  - {entity: archive, effect: removes, with: account}
  - {entity: template, effect: removes, with: account}
  - {entity: budget, effect: removes, with: account}
  - {entity: transaction, as: other-side, effect: creates, to: Unreconciled, facts: [Account, Amount, Date, Value date, Notes]}
  contexts:
    app:
      place: android-app::accounts
---

# Delete an account

## Trigger

The User chooses Delete on an account and confirms that it is deleted with all its transactions.

## Outcome

The account, its transactions, transfers, archives, templates and budgets are gone; the other side of each of its transfers stays in the other account as an ordinary transaction.

## Edge cases

- The other side of a transfer is kept even when it is in a closed account.
- Deletion is refused while the account holds transactions linked to a closed debt.
