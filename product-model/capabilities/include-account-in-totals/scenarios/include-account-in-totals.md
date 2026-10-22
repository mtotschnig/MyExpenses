---
kind: primary
routes:
  app: Android app
steps:
- text: The User unchecks Exclude from totals on an account
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name, Exclude from totals]}
  contexts:
    app:
      place: android-app::accounts
- text: The Product includes the account in totals
  kind: product
  actor: user
  entities:
  - {entity: account, effect: changes, facts: [Exclude from totals]}
  contexts:
    app:
      place: android-app::accounts
---

# Include in totals

## Trigger

The User unchecks Exclude from totals on an account.

## Outcome

The account counts in totals again.
