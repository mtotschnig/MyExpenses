---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses a custom range and its start and end dates
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::budget-editor
- text: The Product saves the budget for that range
  kind: product
  actor: user
  entities:
  - {entity: budget, effect: creates, facts: [Title, Description, Account, Currency, Period, Start date, End date, Filters, Budgeted amount]}
  contexts:
    app:
      place: android-app::budget-editor
---

# Budget for a custom range

## Trigger

The User chooses a custom range instead of a repeating period.

## Outcome

The budget covers the one range.
