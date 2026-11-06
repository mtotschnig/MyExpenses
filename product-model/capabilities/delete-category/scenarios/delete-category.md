---
kind: primary
routes:
  app: Android app
steps:
- text: The User selects categories, chooses Delete and confirms
  kind: actor
  actor: user
  entities:
  - {entity: category, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::categories
- text: The Product deletes each category no transaction or template uses, with its sub categories and the allocations budgets give it
  kind: product
  actor: user
  entities:
  - {entity: category, effect: removes}
  - {entity: budget, effect: changes, facts: [Category allocations]}
  - {entity: transaction, effect: reads, facts: []}
  - {entity: template, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::categories
---

# Delete a category

## Trigger

The User selects categories, chooses Delete and confirms, warned when budgets allocate to them.

## Outcome

The categories, their sub categories and their budget allocations are gone.
