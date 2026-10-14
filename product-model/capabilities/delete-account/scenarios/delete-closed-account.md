---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses Delete on a closed account and confirms
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
  - {entity: account, effect: removes, from: Closed}
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

# Delete a closed account

## Trigger

The User chooses Delete on a closed account; the warning says the account is closed.

## Outcome

The closed account and everything recorded in it are gone.
