---
appliesTo:
- {type: capability, id: create-split-transaction}
- {type: capability, id: transform-to-split}
- {type: capability, id: apply-template}
- {type: entity, id: licence, facts: [Split transaction trial]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/model/ContribFeature.kt#ContribFeature
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BaseMyExpenses.kt#contribFeatureCalled
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/util/licence/LicenceHandler.kt#usagesLeft
---

# New split transactions need a Contrib licence or its trial

New split, Transform to split and applying a split template are available with a Contrib, Extended or Professional licence, or during the 60-day split transaction trial that starts the first time one of them is used.
