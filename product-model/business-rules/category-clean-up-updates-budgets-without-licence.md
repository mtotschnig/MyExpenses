---
appliesTo:
- {type: entity, id: budget, effect: changes, contexts: [{place: 'android-app::categories'}]}
permits:
- actors:
  - user
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/ManageCategoriesViewModel.kt#mergeCategories
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/ManageCategoriesViewModel.kt#deleteCategoriesDo
---

# The User updates budget allocations by deleting or merging categories whatever the licence

Deleting a category removes the allocations budgets give it, and merging categories moves those allocations and budget filters to the kept category, with or without a Professional licence.
