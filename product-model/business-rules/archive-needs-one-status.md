---
appliesTo:
- {type: capability, id: archive-transactions}
- {type: entity, id: archive, effect: creates}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/dialog/ArchiveDialogFragment.kt#ArchiveInfo
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/Archive.kt#archive
---

# An archive holds transactions of one status

An archive is created only for a range with at least one transaction and no archive in it; in an account that supports reconciliation, the transactions apart from void ones must share one status, which the archive takes.
