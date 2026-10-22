---
domain: templates-and-plans
availability:
- {place: android-app}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/service/PlanExecutor.kt#doWork
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryTemplate.kt#instantiateTemplate
---

# Execute due plans

The Product applies due occurrences of plans with automatic execution and notifies the User of the others.
