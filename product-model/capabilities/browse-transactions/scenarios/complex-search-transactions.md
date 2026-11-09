---
kind: alternative
routes:
  app: Android app
steps:
- text: The User opens Complex search, configures criteria rules with match-all or match-any logic, negations, or edits, and applies them
  kind: actor
  actor: user
  entities: []
  contexts:
    app:
      place: android-app::transactions
- text: The Product applies the built criteria set and displays matching transactions and their sums
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Category, Amount, Notes, Party, Payment method, Tags, Date]}
  - {entity: account, effect: reads, facts: []}
  - {entity: category, effect: reads, facts: []}
  - {entity: party, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::transactions
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/compose/filter/FilterDialog.kt#FilterDialog
---

# Complex search transactions

## Trigger

The User searches the list using Complex search.

## Outcome

The custom set of criteria is evaluated using the selected matching rule (all or any) and applied when confirmed.

## Edge cases

- Criteria can be negated (NOT logic) or cleared altogether.
- Closing the dialog with unsaved edits prompts for confirmation to discard changes.
