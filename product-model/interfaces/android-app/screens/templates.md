---
entities:
- {entity: template, shows: [Title, Account, Kind, Amount, Category, Party, Default action, Recurrence, Usage]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/ManageTemplates.kt#ManageTemplates
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/fragment/TemplatesList.kt
---

# Templates and plans

Every template, marking those with a plan, with the actions to apply, edit and delete them.
