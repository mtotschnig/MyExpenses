---
domain: transactions
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryTransaction.kt#createTransfer
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/delegate/TransferDelegate.kt#buildTransaction
---

# Transfer

Money moved from one account to another, kept as one entry in each account. Each side is reconciled against its own account's statement.

## Information kept

- **From account** — the account the money leaves
- **To account** — the account the money arrives in
- **Amount** — the amount leaving the From account, in its currency
- **Received amount** — the amount arriving in the To account, in its currency; the same amount when both accounts share a currency
- **Date** — when it happened, with the time where the person records one
- **Value date** — when it took effect in the account, where it differs from the date
- **Category** — an optional transfer category
- **Notes** — free text
- **Tags** — labels, kept on both sides
- **Equivalent amount** — the value in the home currency
- **Exported** — whether it has been exported
- **Archive** — the archive one of its sides is packed into, if any
- **From status** — Unreconciled, Cleared, Reconciled or Void, as confirmed in the From account
- **To status** — Unreconciled, Cleared, Reconciled or Void, as confirmed in the To account
