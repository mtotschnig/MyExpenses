---
kind: primary
routes:
  app: Android app
steps:
- text: The User selects an account
  kind: actor
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Name, Current balance]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product lists its transactions, transfers, split transactions and archives grouped by period, with the balance and the sums of income, expenses and transfers
  kind: product
  actor: user
  entities:
  - {entity: account, effect: reads, facts: [Opening balance, Current balance, Cleared total, Reconciled total]}
  - {entity: transaction, effect: reads, facts: [Amount, Date, Category, Party, Notes, Tags]}
  - {entity: transfer, effect: reads, facts: [From account, To account, Amount, Date]}
  - {entity: split-transaction, effect: reads, facts: [Amount, Date, Party, Parts]}
  - {entity: archive, effect: reads, facts: [Period, Amount, Status]}
  contexts:
    app:
      place: android-app::transactions
---

# Review an account

## Trigger

The User selects an account.

## Outcome

The account's entries are listed by period with its balance and the sums of income, expenses and transfers.

## Edge cases

- Grouping is by day, week, month, year or none, and sorting by date or amount.
- When the account has a default budget for the shown period, its progress is shown with the list.
