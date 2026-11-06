---
appliesTo:
- {type: capability, id: create-template}
- {type: capability, id: edit-template}
- {type: capability, id: create-transaction}
- {type: entity, id: template, facts: [Recurrence]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryTemplate.kt#updateNewPlanEnabled
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/model/ContribFeature.kt#ContribFeature
---

# Without a Contrib licence there are at most three plans

A recurrence can be given to a template only while fewer than three templates have a plan or the installation has a Contrib, Extended or Professional licence. Without a Professional licence only one split template can be created.
