---
appliesTo:
- {type: capability, id: create-category}
- {type: capability, id: edit-category}
- {type: capability, id: move-category}
- {type: entity, id: category, facts: [Type, Parent category]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/BaseTransactionDatabase.kt
---

# A sub category has the type of its main category

A sub category takes its parent's type when added or moved, follows it when the parent's type changes, and a category is never moved below itself.
