---
appliesTo:
- {type: capability, id: edit-account}
- {type: capability, id: create-transaction}
- {type: capability, id: edit-transaction}
- {type: capability, id: clone-transaction}
- {type: capability, id: delete-transaction}
- {type: capability, id: mark-transaction-cleared}
- {type: capability, id: remap-transactions}
- {type: capability, id: convert-into-transfer}
- {type: capability, id: apply-template}
- {type: capability, id: apply-plan-instance}
- {type: capability, id: export-transactions}
- {type: entity, id: account}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/BaseTransactionDatabase.kt#TRANSACTIONS_SEALED_INSERT_TRIGGER_CREATE
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/BaseTransactionDatabase.kt#TRANSACTIONS_SEALED_UPDATE_TRIGGER_CREATE
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/BaseTransactionDatabase.kt#TRANSACTIONS_SEALED_DELETE_TRIGGER_CREATE
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/BaseTransactionDatabase.kt#ACCOUNTS_SEALED_TRIGGER_CREATE
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BaseMyExpenses.kt#checkSealed
---

# A closed account and its transactions stay unchanged until reopened

While an account is closed, its details are not edited and no transaction, transfer or split transaction in it is recorded, changed, marked, remapped, converted, deleted or applied from a template; the app refers the User to Reopen. Excluding it from totals, deleting the whole account and reopening it stay possible.

## Rationale

Closing an account settles its history, for example after it has been reconciled for the last time.
