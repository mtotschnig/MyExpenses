---
kind: primary
routes:
  app: Android app
steps:
- text: The User selects transactions, chooses Remap, picks the field and its new value and confirms
  kind: actor
  actor: user
  entities:
  - {entity: transaction, effect: reads, facts: [Category]}
  contexts:
    app:
      place: android-app::transactions
- text: The Product sets the chosen category on every selected transaction
  kind: product
  actor: user
  entities:
  - {entity: transaction, effect: changes, facts: [Category]}
  - {entity: category, effect: reads, facts: [Name]}
  contexts:
    app:
      place: android-app::transactions
---

# Remap the category

## Trigger

The User selects transactions, chooses Remap and picks the new category.

## Outcome

Every selected transaction has the new category.

## Edge cases

- Party, payment method and date are remapped the same way; remapping the date can also set the value date.
- Party and payment method are not offered for transfers, and category is not offered for splits.
