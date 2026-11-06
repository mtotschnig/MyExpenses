---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses New account
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::accounts
- text: The User enters the name, currency, account type and opening balance, and optionally the other details
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::account-editor
- text: The Product creates the open account
  kind: product
  actor: user
  entities:
  - {entity: account, effect: creates, to: Open, facts: [Name, Description, Currency, Account type, Opening balance, Color, Goal or limit, Exchange rate, Dynamic exchange rate, Exclude from totals, Active tags]}
  contexts:
    app:
      place: android-app::account-editor
- text: The Product selects the new account
  kind: product
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name, Current balance]}
  contexts:
    app:
      place: android-app::transactions
---

# Add an account

## Trigger

The User chooses New account.

## Outcome

The new open account is selected and its empty list of transactions is shown.

## Edge cases

- For a currency other than the home currency the form asks for the exchange rate.
- A positive goal or limit is a saving goal, a negative one a credit limit.
