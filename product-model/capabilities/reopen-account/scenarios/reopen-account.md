---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Reopen on a closed account
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::accounts
- text: The Product reopens the account
  kind: product
  actor: user
  entities:
  - {entity: account, effect: changes, from: Closed, to: Open, facts: []}
  contexts:
    app:
      place: android-app::accounts
---

# Reopen an account

## Trigger

The User chooses Reopen on a closed account.

## Outcome

The account is open again and its transactions can change.
