---
domain: templates-and-plans
relations:
- {entity: plan-instance, verb: schedules, cardinality: one-to-many}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/entities/Template.kt#Template
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/entities/Plan.kt#Recurrence
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/TransactionDatabase.java
---

# Template

A saved transaction, transfer or split transaction that is applied to record it again. A template with a plan recurs: each occurrence of the plan is a plan instance.

## Information kept

- **Title** — what the template is called; required
- **Account** — the account its transactions are recorded in
- **Kind** — transaction, transfer or split transaction
- **Amount** — the amount recorded, which may be empty for a template that is always edited before saving
- **Category** — the category of a transaction template
- **Party** — the payee or payer
- **Payment method** — how it is paid
- **Notes** — free text
- **Tags** — labels
- **To account** — the receiving account of a transfer template
- **Parts** — the parts of a split template
- **Default action** — Apply and save or Apply and edit, used when the template is tapped
- **Recurrence** — for a plan: once, daily, weekly, monthly, on the last day of the month, yearly or a custom rule set in the calendar
- **Plan start** — the date of the plan's first occurrence
- **Automatic execution** — whether due occurrences are applied without asking
- **Execution in advance** — how many days before its date an occurrence is executed
- **Usage** — how often and when it was last applied
