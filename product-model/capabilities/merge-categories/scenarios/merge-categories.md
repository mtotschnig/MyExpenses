---
kind: primary
routes:
  app: Android app
steps:
- text: The User selects categories, chooses Merge and picks the one to keep
  kind: actor
  actor: user
  entities:
  - {entity: category, as: kept, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::categories
- text: The Product moves transactions, templates, budget allocations and filters to the kept category and deletes the others
  kind: product
  actor: user
  entities:
  - {entity: category, as: merged, effect: removes}
  - {entity: category, as: kept, effect: reads, facts: []}
  - {entity: transaction, effect: changes, facts: [Category]}
  - {entity: template, effect: changes, facts: [Category]}
  - {entity: budget, effect: changes, facts: [Category allocations, Filters]}
  contexts:
    app:
      place: android-app::categories
---

# Merge categories

## Trigger

The User selects categories, chooses Merge and picks the one to keep.

## Outcome

Everything that used the merged categories uses the kept one, and the others are gone.
