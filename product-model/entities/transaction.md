---
domain: transactions
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/entities/Transaction.kt#Transaction
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/TransactionDatabase.java
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/model/CrStatus.kt#CrStatus
---

# Transaction

An expense or an income recorded in one account.

## Information kept

- **Account** — the account it is recorded in
- **Amount** — negative for an expense, positive for an income, in the account's currency
- **Date** — when it happened, with the time where the person records one
- **Value date** — when it took effect in the account, where it differs from the date
- **Category** — the expense or income category, main or sub category
- **Party** — the payee of an expense or the payer of an income
- **Payment method** — such as cheque, credit card, deposit or direct debit
- **Reference number** — such as a cheque number
- **Notes** — free text
- **Tags** — labels used for filtering and reports
- **Original amount** — the amount and currency actually paid, when it differs from the account's currency
- **Equivalent amount** — the value in the home currency
- **Exported** — whether it has been exported
- **Archive** — the archive it is packed into, if any

## States

### Unreconciled

Recorded and not yet confirmed against a statement.

### Cleared

Confirmed as having gone through the account, not yet reconciled.

### Reconciled

Confirmed when the account was balanced against a statement.

### Void

Kept in the list but counted nowhere.
