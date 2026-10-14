---
kind: primary
routes:
  app: Android app
steps:
- text: The User taps the allocation of a category and enters an amount, ticking Set only for the shown period when it applies to that period only
  kind: actor
  actor: user
  entities:
  - {entity: budget, effect: reads, facts: [Category allocations, Available]}
  - {entity: category, effect: reads, facts: []}
  contexts:
    app:
      place: android-app::budget
- text: The Product saves the allocation for every period from the shown one on, or for the shown period only
  kind: product
  actor: user
  entities:
  - {entity: budget, effect: changes, facts: [Category allocations]}
  contexts:
    app:
      place: android-app::budget
---

# Allocate to a category

## Trigger

The User taps a category's allocation and enters an amount.

## Outcome

The category has the allocation, and what is available follows.

## Edge cases

- A sub category's allocation cannot exceed what is left of its main category's allocation.
- An allocation cannot go below what its sub categories have, less the rollover from the previous period.
