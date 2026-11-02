# Search of transactions

The magnifier in the top bar of an account opens the search: a field for text in place of the
title, a row of chips for the kinds of filter below it in place of the balance, and above the list
a line with what was found, e.g. "23 transactions · expenses −412,30 €", with "Reset". The back
arrow ends the search and removes all filters. Portfolios keep the classic page and its filter
dialog.

The diagram [`search.puml`](search.puml) shows how the parts work together. Render it with the
PlantUML plugin of Android Studio or `java -jar plantuml.jar search.puml`.

## One filter, shared with the classic UI

The search does not keep a state of its own. It reads and writes the filter that
`FilterPersistence` stores for each account, the same the classic UI, the export and the sums
use. `TransactionSearch` only remembers whether the search is open and what is being typed; the
text is stored 300 ms after the last key, so the list does not reload for each letter.
`BaseMyExpenses.Page(withFilterUi = false)` leaves out the classic filter dialog and filter card.

## The model

`SearchFilter` reads the stored `Criterion` in one of two forms:

- `Simple`: at most one filter per `SearchKind`, all have to match (AND). Several values of a
  kind, e.g. two categories, are one criterion and either of them matches (OR). Each filter can be
  excluded ("without …"), stored as `NotCriterion`.
- `Advanced`: what the classic UI can build beyond that, e.g. criteria joined by OR, a kind twice
  or nested criteria. It is one chip that can only be removed. Text can still be searched within it.

The kinds are ordered like their chips, so the stored criterion does not depend on the order in
which the user set the filters.

## Free text

`SearchTextCriterion` (in the main module, `provider/filter`) finds the text in the note, the
reference number, the payee and the category, and in the payee or category above them: "Lebensmittel"
finds "Lebensmittel:Bäcker". Payees are compared by their normalized name, without case and
accents. A number, e.g. "12,50", also finds transactions of this amount as expense and income
(`searchText`, `parseAmount`). Only columns of the transactions table are read, so it works on
the table and on the views. `IFNULL` keeps transactions without note when the text is excluded.

The classic UI shows this filter like the others, but can only remove it.

## Choosing values

Each chip opens a sheet (`SearchKindSheet`, built on `FilterSheet` of the design system; from the bottom, in wide windows from the side) with the
choice, a switch to exclude it and the buttons "Reset" and "Show":

- Period: this month, last month, last 30 days, this year, last year, or a custom period whose
  start or end may stay open. Whole months and years show as such on the chip, e.g. "October 2026".
- Amount: expense or income, exactly, at least, at most or between.
- Status: the reconciliation states, for accounts that support them.
- Category, payee, tag, payment method, transfer, account: lists loaded by `loadSearchOptions`,
  with a field to find an option in lists longer than ten. Categories are a tree; picking one also
  finds its subcategories, like the classic filter. "None" finds transactions without category,
  payee or payment method.

Only a note filter made in the classic UI still opens the classic prompt.

Which chips appear depends on the account (`SumInfo`): e.g. categories only if a transaction has
one, accounts only in an aggregate. A filter that is set always keeps its chip.
