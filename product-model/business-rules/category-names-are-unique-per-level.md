---
appliesTo:
- {type: capability, id: create-category}
- {type: capability, id: edit-category}
- {type: entity, id: category, facts: [Name]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/BaseTransactionDatabase.kt#CATEGORY_LABEL_INDEX_CREATE
---

# Category names are unique per level

No two main categories, and no two sub categories of one main category, share a name.
