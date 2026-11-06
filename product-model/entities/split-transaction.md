---
domain: transactions
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryTransaction.kt#createSplitTransaction
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/delegate/SplitDelegate.kt
---

# Split transaction

One payment in one account whose amount is divided into parts, each going to a category or transferred to another account.

## Information kept

- **Account** — the account it is recorded in
- **Amount** — the total, negative for an expense, positive for an income
- **Date** — when it happened, with the time where the person records one
- **Value date** — when it took effect in the account, where it differs from the date
- **Party** — the payee or payer of the whole payment
- **Payment method** — how the whole payment was made
- **Reference number** — such as a cheque number
- **Notes** — free text
- **Tags** — labels used for filtering and reports
- **Parts** — each part a category or a transfer to another account, with its amount, notes and tags; the parts add up to the amount
- **Equivalent amount** — the value in the home currency
- **Exported** — whether it has been exported
- **Archive** — the archive it is packed into, if any

## States

### Unreconciled

Recorded and not yet confirmed against a statement.

### Cleared

Confirmed as having gone through the account; its parts share the status.

### Reconciled

Confirmed when the account was balanced against a statement.

### Void

Kept in the list but counted nowhere.
