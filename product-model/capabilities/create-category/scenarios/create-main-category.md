---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses New main category and enters a name, type and icon
  kind: actor
  actor: user
  entities:
  - {entity: category, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::categories
- text: The Product adds the main category with a suggested color
  kind: product
  actor: user
  entities:
  - {entity: category, effect: creates, facts: [Name, Type, Icon, Color]}
  contexts:
    app:
      place: android-app::categories
---

# Add a main category

## Trigger

The User chooses New main category.

## Outcome

The category is in the tree.
