---
kind: primary
routes:
  app: Android app
steps:
- text: The User chooses Apply and save on a template
  kind: actor
  actor: user
  entities:
  - {entity: template, effect: reads, facts: [Title, Default action]}
  contexts:
    app:
      place: android-app::templates
- text: The Product records the template's transaction dated today and counts the use
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: creates, to: Unreconciled, facts: [Account, Amount, Date, Category, Party, Payment method, Notes, Tags]}
  - {entity: template, effect: changes, facts: [Usage]}
  contexts:
    app:
      place: android-app::templates
---

# Apply and save

## Trigger

The User taps a template whose default action is Apply and save, or selects templates and chooses Apply and save.

## Outcome

A transaction per template is recorded for today.

## Edge cases

- A transfer template between currencies opens the form instead of saving at once.
- Applying a split template needs a Contrib licence or its trial.
