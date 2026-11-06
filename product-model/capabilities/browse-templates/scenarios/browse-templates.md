---
kind: primary
routes:
  app: Android app
steps:
- text: The User opens Templates and plans
  kind: actor
  actor: user
  entities:
  - {entity: template, effect: reads, facts: [Title, Kind, Amount, Recurrence]}
  contexts:
    app:
      place: android-app::templates
- text: The Product lists every template, marking those with a plan
  kind: product
  actor: user
  entities:
  - {entity: template, effect: reads, facts: [Title, Usage, Amount, Account]}
  contexts:
    app:
      place: android-app::templates
---

# Review templates

## Trigger

The User opens Templates and plans.

## Outcome

Every template is listed with its kind, amount and plan.

## Edge cases

- The list sorts by title, usage, last use, amount, next occurrence or account.
- Templates of a closed account are marked as locked.
