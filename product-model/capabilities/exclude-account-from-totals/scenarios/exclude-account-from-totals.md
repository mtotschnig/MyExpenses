---
kind: primary
routes:
  app: Android app
steps:
- text: The User checks Exclude from totals on an account
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name, Exclude from totals]}
  contexts:
    app:
      place: android-app::accounts
- text: The Product excludes the account from totals
  kind: product
  actor: user
  entities:
  - {entity: account, effect: changes, facts: [Exclude from totals]}
  contexts:
    app:
      place: android-app::accounts
---

# Exclude from totals

## Trigger

The User checks Exclude from totals on an account.

## Outcome

The account no longer counts in totals, groups of accounts or their exports.
