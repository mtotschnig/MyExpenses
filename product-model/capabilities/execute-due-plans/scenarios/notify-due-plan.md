---
kind: alternative
routes:
  app: Android app
steps:
- text: The Product finds an open occurrence due today or within its template's days in advance
  kind: condition
  unattended: true
  entities:
  - {entity: plan-instance, effect: reads, facts: [Date]}
  - {entity: template, effect: reads, facts: [Automatic execution]}
  contexts:
    app:
      place: android-app
- text: The Product posts a notification with the account, title, category and amount, offering Apply, Edit and Cancel
  kind: product
  entities:
  - {entity: template, effect: reads, facts: [Title, Amount, Category]}
  - {entity: account, effect: reads, facts: [Name]}
  - {entity: category, effect: reads, facts: []}
  contexts:
    app:
      place: android-app
---

# Notify about a due occurrence

## Trigger

The planner finds a due occurrence of a template without automatic execution.

## Outcome

A notification offers Apply, Edit and Cancel; the occurrence stays open until the User acts.
