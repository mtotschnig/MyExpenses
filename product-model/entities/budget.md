---
domain: budgeting
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/data/Budget.kt#Budget
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/TransactionDatabase.java
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BudgetEdit.kt#onFabClicked
---

# Budget

An amount allocated for spending in one account, or in all accounts of one currency, per period, divided among categories and compared with what is spent.

## Information kept

- **Title** — what the budget is called
- **Description** — free text
- **Account** — one account, or all accounts in one currency
- **Currency** — the currency of its amounts
- **Period** — day, week, month or year, repeating, or one custom range
- **Start date** — the first day of a custom range
- **End date** — the last day of a custom range
- **Filters** — the categories, parties, payment methods, statuses, tags and, across accounts, accounts whose transactions count
- **Default budget** — whether it is the one shown with the account's transactions for its period
- **Budgeted amount** — the total for a period, kept for every period or for one period only
- **Category allocations** — the amount allocated to a main or sub category for a period, kept for every period or for one period only
- **Rollovers** — what is left of a period carried into the next, for the whole budget or per category
- **Spent** — the sum of the matching transactions in the period
- **Available** — the allocation plus rollover minus what was spent
