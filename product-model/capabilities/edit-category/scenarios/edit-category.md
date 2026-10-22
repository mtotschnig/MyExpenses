---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Edit on a category and changes its name, icon or type
  kind: actor
  actor: user
  entities:
  - {entity: category, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::categories
- text: The Product saves the category
  kind: product
  actor: user
  entities:
  - {entity: category, effect: changes, facts: [Name, Icon, Type]}
  contexts:
    app:
      place: android-app::categories
---

# Change a category

## Trigger

The User chooses Edit on a category.

## Outcome

The category shows the changes; a main category's sub categories take its new type.
