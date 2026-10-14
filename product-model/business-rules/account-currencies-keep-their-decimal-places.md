---
appliesTo:
- {type: capability, id: create-account}
- {type: capability, id: edit-account}
- {type: capability, id: import-qif-file}
- {type: capability, id: import-csv-file}
- {type: entity, id: account}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryAccount.kt#createAccount
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryAccount.kt#updateAccount
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryCurrency.kt#storeCustomFractionDigitsSync
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/BaseTransactionDatabase.kt#upgradeTo191
---

# An account's currency keeps its number of decimal places

When an account is created, whether in the account form, as the first account during onboarding or by a QIF or CSV import, and when an account's currency is changed, the app records how many decimal places that currency has at that moment. Amounts in the currency are then always read with that number, even if Android's definition of the currency changes later.

## Rationale

Amounts are stored in the currency's smallest unit, so a change in the number of decimal places would change every amount. Android updates have changed currency definitions before, for example the Hungarian forint's.
