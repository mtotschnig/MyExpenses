---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Edit on a category and changes its name, icon, type or parent category
  kind: actor
  actor: user
  entities:
  - {entity: category, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::categories
- text: The Product saves the category; under a new parent it takes that parent's type, and as a main category it has a color
  kind: product
  actor: user
  entities:
  - {entity: category, effect: changes, facts: [Name, Icon, Type, Parent category, Color]}
  contexts:
    app:
      place: android-app::categories
---

# Change a category

## Trigger

The User chooses Edit on a category.

## Outcome

The category shows the changes; a main category's sub categories take its new type.

## Edge cases

- The parent list leaves out the category itself and its own sub categories.
