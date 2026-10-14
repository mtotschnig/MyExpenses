---
kind: alternative
routes:
  app: Android app
steps:
- text: The User enters the template and chooses a recurrence, the first date, automatic execution and days in advance
  kind: actor
  actor: user
  entities:
  - {entity: template, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::template-editor
- text: The Product saves the template and adds its plan to the planner calendar
  kind: product
  actor: user
  entities:
  - {entity: template, effect: creates, facts: [Title, Account, Kind, Amount, Category, Party, Payment method, Notes, Tags, To account, Parts, Default action, Recurrence, Plan start, Automatic execution, Execution in advance]}
  contexts:
    app:
      place: android-app::template-editor
- text: The Product runs the planner at once, so that an occurrence already due is executed
  kind: product
  actor: user
  entities:
  - {entity: template, effect: reads, facts: [Automatic execution]}
  contexts:
    app:
      place: android-app::template-editor
---

# Create a plan

## Trigger

The User gives the new template a recurrence.

## Outcome

The template has a plan in the planner calendar, and an occurrence already due is executed.

## Edge cases

- A custom recurrence is set in the calendar app after the template is saved.
- Monthly on a day after the 28th can be set to the last day of the month.
