---
appliesTo:
- {type: capability, id: mark-transaction-cleared}
- {type: capability, id: mark-transaction-unreconciled}
- {type: capability, id: balance-account}
- {type: entity, id: account, facts: [Account type]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/model/AccountType.kt#AccountType
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/delegate/TransactionDelegate.kt#configureStatusSpinner
---

# Statuses are tracked only in accounts whose type supports reconciliation

Cleared, reconciled and balancing are offered only for account types that support reconciliation; a cash account, by default, does not.
