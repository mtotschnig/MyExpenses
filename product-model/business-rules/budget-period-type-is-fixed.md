---
appliesTo:
- {type: capability, id: edit-budget}
- {type: entity, id: budget, facts: [Period]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BudgetEdit.kt#populateData
---

# A budget keeps the period type it was created with

The period type is chosen when the budget is created; editing changes its title, description, account, filters and default flag.
