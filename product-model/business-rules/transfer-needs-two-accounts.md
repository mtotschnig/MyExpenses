---
appliesTo:
- {type: capability, id: create-transaction}
- {type: capability, id: convert-into-transfer}
- {type: capability, id: link-transfer}
- {type: entity, id: transfer, effect: creates}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BaseActivity.kt#showTransferAccountMissingMessage
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/delegate/TransferDelegate.kt#updateAccount
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/MyExpensesViewModel.kt#canLinkSelection
---

# A transfer is between two different accounts

A transfer needs a second account; its two accounts are never the same, and a transaction is converted or linked into a transfer only with another account.
