---
appliesTo:
- {type: capability, id: convert-into-transfer}
- {type: entity, id: transfer, effect: creates}
- {type: entity, id: settings, facts: [Default transfer category]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/MoreDbUtils.kt#transformToTransfer
---

# A transaction converted into a transfer keeps its category or gets the default transfer category

The converted transaction loses its payment method; its category, or the default transfer category when it has none, goes to both sides.
