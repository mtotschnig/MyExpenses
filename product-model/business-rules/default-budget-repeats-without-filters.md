---
appliesTo:
- {type: capability, id: create-budget}
- {type: capability, id: edit-budget}
- {type: entity, id: budget, facts: [Default budget]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BudgetEdit.kt#configureFilterDependents
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/BaseTransactionProvider.kt#budgetDefaultSelect
---

# Only a repeating budget without filters can be the default budget

The default budget is the one shown with an account's transactions for its period; it must have a repeating period and no filter.
