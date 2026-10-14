---
domain: accounts
relations:
- {entity: transaction, verb: holds, cardinality: one-to-many}
- {entity: split-transaction, verb: holds, cardinality: one-to-many}
- {entity: transfer, verb: sends, cardinality: one-to-many}
- {entity: transfer, verb: receives, cardinality: one-to-many}
- {entity: archive, verb: holds, cardinality: one-to-many}
- {entity: template, verb: is used by, cardinality: one-to-many}
- {entity: budget, verb: is planned by, cardinality: one-to-many}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/model2/Account.kt#Account
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/BaseTransactionDatabase.kt#ACCOUNTS_CREATE
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/AccountEdit.kt#saveState
---

# Account

A place money is kept — cash, a bank account, a credit card, an asset or a liability — in one currency. Its balance is the opening balance plus every transaction recorded in it that is not void.

## Information kept

- **Name** — what the account is called; required
- **Description** — free text
- **Currency** — the currency every amount in the account is kept in
- **Account type** — Cash, Bank, Credit card, Asset, Liability, Investment or a type the person added; the type says whether the account is an asset and whether it supports reconciliation
- **Opening balance** — the balance before the first recorded transaction
- **Color** — the color the account and its transactions are shown in
- **Goal or limit** — a positive amount is a saving goal, a negative amount a credit limit
- **Exchange rate** — for an account in another currency than the home currency, the rate used to value it in the home currency
- **Dynamic exchange rate** — whether an account in another currency is valued at the latest exchange rate instead of the rates of its transactions
- **Exclude from totals** — whether the account is left out of the totals across accounts
- **Active tags** — tags preselected on new transactions recorded in this account
- **Current balance** — opening balance plus every transaction that is not void
- **Cleared total** — opening balance plus the transactions that are cleared or reconciled
- **Reconciled total** — opening balance plus the reconciled transactions

## States

### Open

Transactions are recorded and changed freely.

### Closed

The account, its details and its transactions are read-only until it is reopened.
