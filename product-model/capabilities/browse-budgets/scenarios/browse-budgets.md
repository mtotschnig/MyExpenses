---
kind: primary
routes:
  app: Android app
steps:
- text: The User opens Budgeting
  kind: actor
  actor: user
  entities:
  - {entity: budget, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
- text: Without a Professional licence, the Product starts the 60-day trial on first use
  kind: product
  actor: user
  entities:
  - {entity: licence, effect: changes, facts: [Budget trial]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product lists every budget with what is budgeted, spent and available in its current period
  kind: product
  actor: user
  entities:
  - {entity: budget, effect: reads, facts: [Title, Account, Period, Budgeted amount, Spent, Available]}
  contexts:
    app:
      place: android-app::budgets
---

# Open Budgeting

## Trigger

The User opens Budgeting from the main menu.

## Outcome

Every budget is listed with its current period's figures.

## Edge cases

- Budgets are grouped by account or by period type.
- Without a licence, each opening during the trial first shows how long the trial lasts and offers the licence.
