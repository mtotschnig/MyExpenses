# Ledger and facts

The analyses of the new UI, e.g. the contracts and the balance,
all work on the same transactions. They are read once into a `Ledger`, with the tags of each
transaction; the categories and tags of the user are read once into a `Catalog`. Each feature derives what it needs from the
ledger and offers what others need to know as **facts**: plain, immutable data in its own package.
A feature reads the facts of other areas from their data modules, never their view models.

The diagrams are PlantUML sources next to this file:

- [`architecture.puml`](architecture.puml): which repository and view model reads what, from level 0 down
- [`model.puml`](model.puml): the shared types `LedgerSource`, `Ledger`, `Catalog` and `AnalysisState`

Render them with the PlantUML plugin of Android Studio or `java -jar plantuml.jar *.puml`.

Ledger and facts are computed once for the process, in the **repositories** of the data modules,
which `NextData` in the shell creates on first use: they outlive the activity, so widgets and
notifications can read them without a screen. A repository only reads what the levels above
offer; a view model shows the flows of its repository and changes its store.

| Repository | Reads | Offers |
| --- | --- | --- |
| `ProviderLedgerSource` (shell) | transactions with their tags and split parts, own accounts with their IBANs, categories, tags, consent | `LedgerSource`: consent, `Ledger`, `Catalog`, today |
| `ContractRepository` | ledger, its rules | `ContractFacts`: contracts, incomes, ids of their payments; to `ContractsViewModel` the analysis and the overviews |
| `BalanceRepository` | ledger, `ContractFacts` | the balance from salary to salary |

The view models without a repository offer nothing to others and read what they show
themselves:

| View model | Reads |
| --- | --- |
| `FixedTransactionsViewModel` (package `transactions`) | the transactions it is asked to show |

The view models live as long as `MyExpensesNext`. The entry of each area in the shell (`…Feature`,
listed in `NextFeatures`) creates them with `viewModelsOf { … }`
and hands it everything it needs as constructor parameters: its repository or the `LedgerSource`
and the facts it reads from `NextData`, its store, and what it takes from the app's dependency graph
(`injector.repository()`, …). The dynamic feature cannot add its types to the app's dependency
graph, so this is done by hand. No view model of the new UI uses field injection, so they can start
working in `init` and be tested with a `FakeLedgerSource` and `withTestDataStore`, and their
repositories with the `backgroundScope` of `runTest`, see `BalanceViewModelTest`.

## Where the code lives

The shared types are in `:next:core:model` (plain Kotlin), the helpers without Compose, with all
texts, in `:next:core:common` and what screens share in `:next:core:ui`. Each area has two modules:
`:next:data:<name>` with its facts, analyses and store, without Compose, which may build on the data
modules of other areas, and `:next:feature:<name>` with its screens and view models, which depends
on data and core modules but never on another feature. A screen of another area, e.g. the incomes
in the balance, is handed in by the shell as a composable parameter. Nothing of this knows
`:myExpenses`. The shell `:ui_next` knows the app: `ProviderLedgerSource` reads the ledger,
the entries of the areas (`…Feature`) create the view models, and small adapters implement what features need of the
app, e.g. `AppFixedTransactions`. The
dependencies between the modules are in [`modules.puml`](../../../../../../../../../../../../../ui_next/modules.puml).

## Computing only when something changed

An analysis is computed again only when what it depends on has changed, not each time its screen
is shown again, e.g. after editing a transaction or when the app comes back from the background.

- Each analysis is a pure function of an **input**: a data class with everything its result depends
  on, e.g. `AnalysisInput(transactions, rules, today)` of the contracts. Equal inputs give the same
  result, so the input is the key; nothing has to tell when a result is outdated.
- The repository or view model keeps a `Memo` per analysis, with the last input and its result.
  Their flows stop a few seconds after nothing collects them (`shareWhileShown`), the memo stays,
  so a restart with an equal input gives the result at once; the memos of repositories stay for
  the process, also when the activity is recreated. `mapMemo` also passes over an input
  equal to the previous one, null stays null.
- The input holds only what the analysis reads. The contracts do not read the tags of the
  transactions, so tagging a transaction does not analyse the contracts again.
- `Ledger`, `LedgerTransaction`, `OwnAccounts`, inputs and facts are data classes, so that equal
  data compares equal. A class compared by identity is never wrong, it is only computed more often.
- `ProviderLedgerSource` reads the transactions again on each change of the database. Changes closer
  than 50 ms, e.g. of a transaction and its tags, lead to one ledger, the ledger itself is built in
  a `Memo`, and on a restart the last ledger stays until the new one is read.
- Work with side effects, e.g. the automatic decisions about contracts, runs only for a new result.

## Rules

The rules for features, facts, view models and screens are in
[`ui_next/ARCHITECTURE.md`](../../../../../../../../../../../../../ui_next/ARCHITECTURE.md), sections P, A, V and S.
The skill `architektur-pruefen` checks code against them.

## Adding a feature

1. Create its modules `:next:data:<name>` with the plugin `next.data` and `:next:feature:<name>`
   with `next.feature` (see `build-logic`), include them in `settings.gradle` and let `:ui_next`
   depend on both. Derive its input from the `Ledger` with a pure function in the data module and
   test it there.
2. If other features, a widget or a notification need to know something from it, write a
   repository in its data module: give it the `LedgerSource`, one constructor parameter per kind of
   facts it reads, e.g. `contractFacts: Flow<ContractFacts?>`, its store and a `CoroutineScope`,
   compute its result with a `Memo` of an input data class, see above, add a facts class and
   expose it with `shareWhileShown(scope)`, so that it is computed once. Create it in `NextData`.
3. Write its view model as a plain `ViewModel`. Give it its repository, or the `LedgerSource` and
   the facts it reads if it offers nothing, and its store. It shows the flows with
   `stateWhileShown(viewModelScope, …)`. Automatic decisions, e.g. applying rules, belong into the
   repository as a shared flow with the side effect, collected by `NextData.applyAutomaticDecisions`
   (rule `analysis-decisions-in-repo`), never into `init` of a view model.
4. Write its entry composable (`…Flow`) and, if it has several actions, an `…Actions` interface.
   What it needs of the app, e.g. writing to the database, it declares as an interface of its own;
   the shell implements it.
5. Create the view model with `viewModelsOf` in the entry of the area in the shell (`…Feature`, add
   it to `NextFeatures`), put its entry composable on a page there (`pages`, with a `NextKey`), and
   add it to this README and to `architecture.puml`, with its repository if it has one.

If the feature needs a field of the transactions the ledger does not read yet, add it to
`LedgerTransaction` and to the projection of `ProviderLedgerSource` rather than writing another query.

## What stays outside

The ledger is the read model of the analyses over the last `HISTORY_MONTHS` months, not a
replacement for the provider. These read the database themselves:

- the transaction lists of accounts with paging, search and filter, and `FixedTransactionsViewModel`
- the accounts of the overview, from `MyExpensesV2ViewModel`; `ProviderLedgerSource` reads the
  own accounts itself, with their labels and types
