---
domain: templates-and-plans
availability:
- {place: android-app}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/fragment/TemplatesList.kt#dispatchCommandMultiple
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/TemplatesListViewModel.kt#updateDefaultAction
---

# Change template default action

The User sets whether tapping templates saves the transaction at once or opens it for editing.
