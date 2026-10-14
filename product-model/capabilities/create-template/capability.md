---
domain: templates-and-plans
availability:
- {place: android-app}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/delegate/TransactionDelegate.kt#syncStateAndValidate
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/TransactionEditViewModel.kt#save
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BaseMyExpenses.kt#createTemplate
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryTemplate.kt#updateNewPlanEnabled
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryPlan.kt#createPlan
---

# Create template

The User saves a transaction, transfer or split transaction as a template, optionally with a plan that makes it recur.
