---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Edit on a budget
  kind: actor
  actor: user
  entities:
  - {entity: budget, effect: reads, facts: [Title]}
  contexts:
    app:
      place: android-app::budget
- text: The User changes its details and saves
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::budget-editor
- text: The Product saves the budget
  kind: product
  actor: user
  entities:
  - {entity: budget, effect: changes, facts: [Title, Description, Account, Filters, Default budget, Start date, End date]}
  contexts:
    app:
      place: android-app::budget-editor
---

# Change a budget

## Trigger

The User chooses Edit on a budget.

## Outcome

The budget shows the changes.

## Edge cases

- The period type and the budgeted amount are not changed in this form.
