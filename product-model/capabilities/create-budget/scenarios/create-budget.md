---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses New budget
  kind: actor
  actor: user
  entities:
  - {entity: budget, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::budgets
- text: The User enters the title, picks an account or a currency, the period type, the budgeted amount and optional filters
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::budget-editor
- text: The Product saves the budget with the amount as the budgeted amount of every period
  kind: product
  actor: user
  entities:
  - {entity: budget, effect: creates, facts: [Title, Description, Account, Currency, Period, Start date, End date, Filters, Default budget, Budgeted amount]}
  contexts:
    app:
      place: android-app::budget-editor
---

# Create a budget

## Trigger

The User chooses New budget.

## Outcome

The budget is listed with its budgeted amount for each period.

## Edge cases

- A budget for all accounts of one currency can be filtered to some of its accounts.
- Default budget is offered only for a repeating period and while no filter is set.
