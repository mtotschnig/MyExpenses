---
domain: categories
availability:
- {place: android-app}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/ManageCategoriesViewModel.kt#saveCategory
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryCategory.kt#saveCategory
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/compose/CategoryDialogs.kt#CategoryEdit
---

# Edit category

The User renames a category, changes its icon or type, or picks another parent category for it.
