---
kind: primary
routes:
  app: Android app
steps:
- text: The Product finds open occurrences due today or within their template's days in advance
  kind: condition
  unattended: true
  entities:
  - {entity: plan-instance, effect: reads, facts: [Date]}
  - {entity: template, effect: reads, facts: [Execution in advance, Automatic execution]}
  contexts:
    app:
      place: android-app
- text: For a template with automatic execution, the Product records its transaction on the occurrence's date and marks the occurrence applied
  kind: product
  entities:
  - {entity: transaction, effect: creates, to: Unreconciled, facts: [Account, Amount, Date, Category, Party, Payment method, Notes, Tags]}
  - {entity: plan-instance, effect: changes, from: Open, to: Applied, facts: [Transaction]}
  - {entity: template, effect: reads, facts: []}
  contexts:
    app:
      place: android-app
- text: The Product posts a notification that opens the recorded transaction
  kind: product
  entities:
  - {entity: transaction, effect: reads, facts: [Amount, Category]}
  contexts:
    app:
      place: android-app
---

# Apply a due occurrence automatically

## Trigger

The planner runs once a day, and right after a plan is saved.

## Outcome

A transaction is recorded for each open occurrence of an automatic plan due today or within its days in advance, and a notification shows it.
