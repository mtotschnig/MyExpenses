---
kind: alternative
routes:
  app: Android app
steps:
- text: The User chooses Export on a group of accounts, optionally merging them into one file
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product writes the transactions of each account in the group that is not excluded from totals and marks them exported
  kind: product
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name, Exclude from totals]}
  - {entity: transaction, effect: changes, facts: [Exported]}
  contexts:
    app:
      place: android-app::export
---

# Export several accounts

## Trigger

The User exports while all accounts, one currency, or a group of accounts is shown.

## Outcome

One file per account, or one merged file, covers every account in the group that is counted in totals.
