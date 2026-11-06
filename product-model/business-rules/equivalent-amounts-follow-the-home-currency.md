---
appliesTo:
- {type: capability, id: change-home-currency}
- {type: entity, id: transaction, facts: [Equivalent amount]}
- {type: entity, id: settings, facts: [Home currency]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/PreferenceActivity.kt
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/PriceCalculationViewModel.kt
---

# Equivalent amounts are kept in the home currency

Changing the home currency recalculates the price history and every equivalent amount in the new currency.
