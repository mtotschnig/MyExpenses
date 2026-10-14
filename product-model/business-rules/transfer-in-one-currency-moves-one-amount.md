---
appliesTo:
- {type: entity, id: transfer, facts: [Amount, Received amount]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/delegate/TransferDelegate.kt#buildTransaction
---

# Within one currency a transfer receives what it sends

When both accounts share a currency, the received amount is the sent amount; between currencies both amounts are kept and give the exchange rate.
