---
domain: templates-and-plans
availability:
- {place: android-app}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/fragment/TemplatesList.kt#dispatchDeleteDo
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/ContentResolvingAndroidViewModel.kt#deleteTemplates
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryTemplate.kt#deleteTemplate
---

# Delete template

The User deletes templates with their plans.
