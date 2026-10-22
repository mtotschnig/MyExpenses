---
appliesTo:
- {type: capability, id: create-account}
- {type: capability, id: import-qif-file}
- {type: entity, id: account, effect: creates}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/model/ContribFeature.kt#ContribFeature
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/util/licence/LicenceHandler.kt#updateNewAccountEnabled
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/ImportDataViewModel.kt#insertAccounts
---

# Without a Contrib licence there are at most five accounts

Creating an account, by hand or by importing, is refused once five accounts exist and the installation has no Contrib, Extended or Professional licence.
