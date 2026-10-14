---
domain: accounts
availability:
- {place: android-app}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/AccountEdit.kt#saveState
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/compose/accounts/AccountList.kt
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/BaseTransactionDatabase.kt#ACCOUNTS_SEALED_TRIGGER_CREATE
---

# Edit account

The User changes the details of an open account.
