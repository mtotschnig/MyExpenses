---
kind: alternative
routes:
  app: Android app
steps:
- text: The User keeps or changes the suggested name and chooses the currency, opening balance and, under more options, the type, description and color
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::onboarding
- text: The Product creates the open account, named after the suggestion when the name is left empty
  kind: product
  actor: user
  entities:
  - {entity: account, effect: creates, to: Open, facts: [Name, Description, Currency, Account type, Opening balance, Color]}
  contexts:
    app:
      place: android-app::onboarding
---

# Set up the first account

## Trigger

The User starts the app for the first time.

## Outcome

The first account exists and the app opens on it.
