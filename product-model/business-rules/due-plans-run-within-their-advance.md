---
appliesTo:
- {type: capability, id: execute-due-plans}
- {type: entity, id: template, facts: [Execution in advance, Automatic execution]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/service/PlanExecutor.kt#doWork
---

# Due occurrences are executed on their day or within their days in advance

The planner runs daily and executes an open occurrence once it is due today or within the template's days in advance; occurrences of templates whose account is closed are left open.
