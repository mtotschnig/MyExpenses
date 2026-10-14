---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses New sub category on a category and enters a name and icon
  kind: actor
  actor: user
  entities:
  - {entity: category, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::categories
- text: The Product adds the sub category with its parent's type
  kind: product
  actor: user
  entities:
  - {entity: category, effect: creates, facts: [Name, Parent category, Type, Icon]}
  contexts:
    app:
      place: android-app::categories
---

# Add a sub category

## Trigger

The User chooses New sub category on a category.

## Outcome

The sub category is under its parent and has the parent's type.
