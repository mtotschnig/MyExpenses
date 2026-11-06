---
kind: edge
routes:
  app: Android app
steps:
- text: The Product finds an open occurrence due today
  kind: condition
  unattended: true
  entities:
  - {entity: plan-instance, effect: reads, facts: [Date]}
  contexts:
    app:
      place: android-app
- text: The template's account is closed, so the Product leaves the occurrence open
  kind: product
  entities:
  - {entity: account, effect: reads, facts: []}
  - {entity: template, effect: reads, facts: []}
  contexts:
    app:
      place: android-app
---

# Closed account

## Trigger

The planner finds a due occurrence of a template whose account is closed.

## Outcome

Nothing is recorded or notified; the occurrence stays open.
