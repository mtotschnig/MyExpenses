---
kind: validation
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
- text: Five accounts exist and the installation has no Contrib licence
  kind: condition
  entities:
  - {entity: licence, effect: reads, facts: [Licence]}
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::accounts
- text: The Product says the current licence allows five accounts and offers a licence
  kind: product
  actor: user
  entities:
  - {entity: licence, effect: reads, facts: [Licence]}
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::accounts
---

# Free account limit reached

## Trigger

The User chooses New account while five accounts exist and there is no Contrib licence.

## Outcome

No account is created; the licence is offered.
