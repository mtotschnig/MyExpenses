---
kind: alternative
routes:
  app: Android app
steps:
- text: The User taps a template with a plan
  kind: actor
  actor: user
  entities:
  - {entity: template, effect: reads, facts: [Title, Recurrence]}
  contexts:
    app:
      place: android-app::templates
- text: The Product shows the plan's occurrences by month, each marked open, applied or cancelled
  kind: product
  actor: user
  entities:
  - {entity: plan-instance, effect: reads, facts: [Date]}
  contexts:
    app:
      place: android-app::plan-calendar
---

# Open one plan

## Trigger

The User taps a template with a plan.

## Outcome

The plan's occurrences are shown by month with their state.
