---
domain: templates-and-plans
availability:
- {place: android-app}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/fragment/TemplatesList.kt
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/TemplatesListViewModel.kt#newFromTemplate
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryTemplate.kt#instantiateTemplate
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/ExpenseEdit.kt
---

# Apply template

The User records a transaction from a template, saving it at once or editing it first.
