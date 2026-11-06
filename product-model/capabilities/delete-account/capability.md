---
domain: accounts
availability:
- {place: android-app}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BaseMyExpenses.kt#confirmAccountDelete
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/ContentResolvingAndroidViewModel.kt#deleteAccountsInternal
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryAccount.kt#deleteAccount
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryAccount.kt#updateTransferPeersForTransactionDelete
---

# Delete account

The User deletes an account together with everything recorded in it.
