---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses New template and its kind
  kind: actor
  actor: user
  entities:
  - {entity: template, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::templates
- text: The User enters the title, account, amount and details, and the default action
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::template-editor
- text: The Product saves the template
  kind: product
  actor: user
  entities:
  - {entity: template, effect: creates, facts: [Title, Account, Kind, Amount, Category, Party, Payment method, Notes, Tags, To account, Parts, Default action]}
  contexts:
    app:
      place: android-app::template-editor
---

# Create a template

## Trigger

The User chooses New template and its kind.

## Outcome

The template is listed, ready to apply.

## Edge cases

- Without a Professional licence only one split template can be created.
