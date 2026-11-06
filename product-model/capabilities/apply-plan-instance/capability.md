---
domain: templates-and-plans
availability:
- {place: android-app}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/PlannerViewModel.kt#applyBulk
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/TemplatesListViewModel.kt#newFromTemplate
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryTemplate.kt#linkTemplateWithTransaction
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/service/PlanNotificationClickHandler.kt
---

# Apply plan instance

The User records the transaction for an open occurrence of a plan, at once or after editing it.
