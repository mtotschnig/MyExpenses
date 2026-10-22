---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Edit on an open account
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::accounts
- text: The User changes its details and saves
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::account-editor
- text: The Product saves the account
  kind: product
  actor: user
  entities:
  - {entity: account, effect: changes, facts: [Name, Description, Currency, Account type, Opening balance, Color, Goal or limit, Exchange rate, Dynamic exchange rate, Exclude from totals, Active tags]}
  contexts:
    app:
      place: android-app::account-editor
---

# Change account details

## Trigger

The User chooses Edit on an open account.

## Outcome

The account keeps the new details; balances follow a changed opening balance.

## Edge cases

- The dynamic exchange rate is offered only for an account in another currency than the home currency.
