---
kind: primary
routes:
  app: Android app
steps:
- text: The User opens the list of accounts
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name, Currency, Account type, Current balance, Goal or limit, Exclude from totals]}
  contexts:
    app:
      place: android-app::accounts
- text: The Product shows each account with its balance and the totals across accounts, leaving out those excluded from totals
  kind: product
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Current balance, Exclude from totals, Currency]}
  - {entity: settings, effect: reads, facts: [Home currency]}
  contexts:
    app:
      place: android-app::accounts
---

# See accounts and totals

## Trigger

The User opens the app or the list of accounts.

## Outcome

Each account shows its current balance; totals per currency and in the home currency leave out accounts excluded from totals.

## Edge cases

- Accounts are grouped by type, currency or flag, and closed accounts are marked as locked.
- An account with a saving goal or credit limit shows how far it is from it.
