---
appliesTo:
- {type: capability, id: delete-category}
- {type: entity, id: category, effect: removes}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/ManageCategoriesViewModel.kt#deleteCategoriesDo
---

# A category used by transactions or templates is not deleted

Deleting skips each category that a transaction or template uses; budget allocations to a deleted category go with it.
