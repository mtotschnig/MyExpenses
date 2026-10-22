---
kind: validation
routes:
  app: Android app
steps:
- text: The User opens the actions of a closed account
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::accounts
- text: The Product offers no Edit for a closed account
  kind: condition
  entities:
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::accounts
---

# Closed account not editable

## Trigger

The User opens the actions of a closed account.

## Outcome

Edit is not offered until the account is reopened.
