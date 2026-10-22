---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Edit on a template
  kind: actor
  actor: user
  entities:
  - {entity: template, effect: reads, facts: [Title]}
  contexts:
    app:
      place: android-app::templates
- text: The User changes its details and saves
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::template-editor
- text: The Product saves the template and updates its plan
  kind: product
  actor: user
  entities:
  - {entity: template, effect: changes, facts: [Title, Account, Kind, Amount, Category, Party, Payment method, Notes, Tags, To account, Parts, Default action, Automatic execution, Execution in advance]}
  contexts:
    app:
      place: android-app::template-editor
---

# Change a template

## Trigger

The User chooses Edit on a template.

## Outcome

The template, and the title and description of its plan, show the changes.

## Edge cases

- A template without a plan can be given one while editing.
