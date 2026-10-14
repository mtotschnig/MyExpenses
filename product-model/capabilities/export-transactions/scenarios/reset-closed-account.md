---
kind: validation
routes:
  app: Android app
steps:
- text: The User chooses Export and reset on a closed account
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product writes the file, deletes nothing and names the reason
  kind: condition
  entities:
  - {entity: account, effect: reads, facts: []}
  - {entity: transaction, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::export
---

# Closed account not reset

## Trigger

The User exports and resets a closed account, or one whose transfers refer to a closed account.

## Outcome

The file is written but nothing is deleted.
