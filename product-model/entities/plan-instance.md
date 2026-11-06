---
domain: templates-and-plans
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/data/PlanInstance.kt#PlanInstanceState
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/TransactionDatabase.java
---

# Plan instance

One dated occurrence of a template's plan.

## Information kept

- **Date** — the day the occurrence falls on
- **Transaction** — the transaction recorded for it, once applied

## States

### Open

Due or upcoming, with no transaction recorded for it.

### Applied

A transaction has been recorded for it.

### Cancelled

Skipped; no transaction is recorded for it.
