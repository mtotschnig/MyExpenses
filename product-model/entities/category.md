---
domain: categories
relations:
- {entity: transaction, verb: classifies, cardinality: one-to-many}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryCategory.kt#saveCategory
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/TransactionDatabase.java
---

# Category

A main or sub category that classifies expenses and incomes; categories form a tree.

## Information kept

- **Name** — unique among the main categories, or among the sub categories of one parent
- **Parent category** — the main category a sub category belongs to
- **Type** — expense, income, both, or transfer; a sub category has its parent's type
- **Icon** — an optional icon
- **Color** — the color of a main category
