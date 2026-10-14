---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Close on an open account
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::accounts
- text: The Product closes the account
  kind: product
  actor: user
  entities:
  - {entity: account, effect: changes, from: Open, to: Closed, facts: []}
  contexts:
    app:
      place: android-app::accounts
---

# Close an account

## Trigger

The User chooses Close on an open account.

## Outcome

The account is closed and listed with the closed accounts.
