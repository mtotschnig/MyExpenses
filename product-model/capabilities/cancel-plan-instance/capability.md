---
domain: templates-and-plans
availability:
- {place: android-app}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/TemplatesListViewModel.kt#cancel
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/fragment/TemplatesList.kt#dispatchCancelInstance
---

# Cancel plan instance

The User skips an occurrence of a plan; a transaction already recorded for it is deleted.
