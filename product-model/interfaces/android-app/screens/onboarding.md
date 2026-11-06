---
entities:
- {entity: account, collects: [Name, Description, Currency, Account type, Opening balance, Color]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/fragment/OnboardingDataFragment.kt#buildAccount
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/OnboardingActivity.kt
---

# Welcome

The first-run setup, where the person creates their first account.
