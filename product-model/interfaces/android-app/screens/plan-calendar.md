---
entities:
- {entity: template, shows: [Title, Recurrence]}
- {entity: plan-instance, shows: [Date]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/fragment/PlanMonthFragment.kt#PlanMonthFragment
---

# Plan calendar

The occurrences of one plan by month, each marked open, applied or cancelled.
