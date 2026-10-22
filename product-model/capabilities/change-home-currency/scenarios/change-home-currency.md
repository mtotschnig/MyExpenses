---
kind: primary
routes:
  app: Android app
steps:
- text: The User picks another home currency and confirms that equivalent amounts are recalculated
  kind: actor
  actor: user
  entities:
  - {entity: settings, effect: reads, facts: [Home currency]}
  contexts:
    app:
      place: android-app::settings
- text: The Product recalculates the price history and the equivalent amount of every transaction in the new currency and makes it the home currency
  kind: product
  actor: user
  entities:
  - {entity: settings, effect: changes, facts: [Home currency]}
  - {entity: transaction, effect: changes, facts: [Equivalent amount]}
  contexts:
    app:
      place: android-app::settings
---

# Change the home currency

## Trigger

The User picks another home currency under Settings, Data, and confirms the recalculation.

## Outcome

Totals and equivalent amounts are in the new home currency.
