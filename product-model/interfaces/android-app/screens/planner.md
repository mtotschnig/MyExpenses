---
entities:
- {entity: template, shows: [Title, Account, Amount]}
- {entity: plan-instance, shows: [Date, Transaction]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/fragment/PlannerFragment.kt#configureMenuInternalPlanInstances
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/PlannerViewModel.kt#applyBulk
---

# Planner

The upcoming occurrences of every plan, with their state, to apply, cancel or reset them.
