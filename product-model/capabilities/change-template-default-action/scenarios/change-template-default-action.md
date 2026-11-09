---
kind: primary
routes:
  app: Android app
steps:
- text: The User selects templates and chooses the default action
  kind: actor
  actor: user
  entities:
  - {entity: template, effect: reads, facts: [Title, Default action]}
  contexts:
    app:
      place: android-app::templates
- text: The Product sets the default action of the selected templates
  kind: product
  actor: user
  entities:
  - {entity: template, effect: changes, facts: [Default action]}
  contexts:
    app:
      place: android-app::templates
---

# Set the default action

## Trigger

The User selects templates and chooses Apply and save or Apply and edit as default action.

## Outcome

Tapping those templates now does the chosen action.

## Edge cases

- Shortcuts pinned for those templates follow the new default action.
