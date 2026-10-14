---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Move on a category and picks the new parent or none
  kind: actor
  actor: user
  entities:
  - {entity: category, effect: reads, facts: [Name, Parent category]}
  contexts:
    app:
      place: android-app::categories
- text: The Product moves the category, giving it the new parent's type, or a color when it becomes a main category
  kind: product
  actor: user
  entities:
  - {entity: category, effect: changes, facts: [Parent category, Type, Color]}
  contexts:
    app:
      place: android-app::categories
---

# Move a category

## Trigger

The User chooses Move on a category and picks its new parent, or none.

## Outcome

The category is under its new parent, with that parent's type, or is a main category with a color.
