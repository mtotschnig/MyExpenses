---
kind: alternative
routes:
  app: Android app
steps:
- text: The User selects all accounts, one currency, or a group of accounts
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
- text: The Product lists the transactions of each account in the group that is not excluded from totals, in the home currency where currencies differ
  kind: product
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Exclude from totals, Currency]}
  - {entity: transaction, effect: reads, facts: [Amount, Equivalent amount]}
  - {entity: settings, effect: reads, facts: [Home currency]}
  contexts:
    app:
      place: android-app::transactions
---

# Review all accounts

## Trigger

The User selects all accounts, one currency, or a group of accounts by type or flag.

## Outcome

Entries of every account in the group that is counted in totals are listed together.
