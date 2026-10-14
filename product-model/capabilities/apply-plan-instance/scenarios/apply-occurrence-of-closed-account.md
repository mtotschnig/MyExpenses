---
kind: validation
routes:
  app: Android app
steps:
- text: The User chooses Apply and save on an occurrence of a locked plan
  kind: actor
  actor: user
  entities:
  - {entity: plan-instance, effect: reads, facts: [Date]}
  contexts:
    app:
      place: android-app::planner
- text: The Product says the plan refers to a closed account
  kind: condition
  entities:
  - {entity: account, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::planner
---

# Closed account

## Trigger

The User applies an occurrence of a plan whose account is closed.

## Outcome

Nothing is recorded.
