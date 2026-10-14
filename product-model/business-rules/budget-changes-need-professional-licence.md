---
appliesTo:
- {type: entity, id: budget, effect: changes, contexts: [{place: 'android-app::budgets'}, {place: 'android-app::budget'}, {place: 'android-app::budget-editor'}]}
permits:
- actors:
  - user
  when:
  - {entity: licence, fact: Licence, is: Professional}
- actors:
  - user
  when:
  - {entity: licence, fact: Budget trial, is-not: Ended}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/model/ContribFeature.kt#ContribFeature
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BaseMyExpenses.kt#contribFeatureCalled
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/util/licence/LicenceHandler.kt#hasTrialAccessTo
---

# Only a Professional licence or the budget trial changes budgets

Changing budgets in Budgeting needs a Professional licence, or the budget trial, which runs for 60 days from the first time Budgeting is opened.
