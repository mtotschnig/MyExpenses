# Architecture of the new UI

This file holds the rules for the modules of the new UI: the shell `:ui_next`, the modules below
`next/` (`:next:core:*`, the data of the areas `:next:data:*` and the features `:next:feature:*`),
`:designsystem`, `:charts`, `:ocr-model`, `:mlkit-core`, `:fixturecollector`, and what the new UI
changes in `:myExpenses`.
Each rule has a tag, so that a review can name it and a test can say which rule it guards.

How a rule is checked:

- **Konsist**: guarded by a test in `:architecture-test`, run with `./gradlew :architecture-test:test`
- **Gradle**: the build fails, e.g. a dependency that is not allowed cannot be resolved
- **test**: mechanical, to be guarded by a Konsist or Gradle test, not written yet
- **review**: needs judgement, checked in code review

The READMEs of the packages explain how a feature works. A rule is written here once and the
READMEs link to it, so that it cannot drift.

## Modules

The diagram [`modules.puml`](modules.puml) shows the dependencies between the modules. It is
generated from the build files with `./gradlew :ui_next:moduleGraph` (`ModuleGraphPlugin` in
`build-logic`) and never edited by hand. An edge that follows from a longer path is left out.
Render it with the PlantUML plugin of Android Studio or `java -jar plantuml.jar modules.puml`.

### `mod-diagram` — Module diagram is generated, never edited by hand

`:ui_next:checkModuleGraph`, part of `check`, fails until the diagram is regenerated after a
dependency change. A new edge needs a reason in the commit message.

**Why:** A hand-edited diagram drifts from the actual dependencies. Generating it keeps it
trustworthy as documentation.

**Check:** Gradle

---

---

### `mod-ds-pure` — `:designsystem` depends only on libraries

It does not depend on `:charts`, so that screens without charts do not get them. It knows no type
of the app (no `Transaction`, `Contract`, `Money`, …), only strings, numbers, icons and lambdas.

**Why:** The design system is the shared vocabulary of all screens. Polluting it with domain types
or optional chart dependencies would force every screen to carry both, and make the design system
harder to reuse or test.

**Check:** Konsist

---

---

### `mod-upstream-hooks` — `:myExpenses` gets only small, additive hooks for the new UI

An optional parameter with a default, a member made public, a callback. No logic of the new UI
goes there, and no code of the main module is copied into `:ui_next` to avoid such a hook.

**Why:** The new UI is a fork-in-progress alongside the existing UI. Keeping changes to the main
module minimal makes it easier to track upstream and to submit some changes upstream later.

**Check:** review

---

---

### `mod-feature-isolated` — Feature modules do not depend on each other

A feature module `:next:feature:*` holds the screens and view models of an area. It depends only
on the core modules `:next:core:*`, on data modules `:next:data:*`, on libraries and on the modules
without app types (`:charts`, `:ocr-model`, `:item-group-model`), never on another feature,
`:myExpenses` or `:ui_next`. A screen of another area, e.g. the incomes in the balance or the
contract of a payment in a saving tip, is handed in by the entry of the area in the shell
(`…Feature`) as a composable parameter. The plugin `next.feature` fails on such a dependency.

**Why:** Features that depend on each other can only be tested and shipped together. Keeping them
isolated means each feature can be built, reviewed and replaced independently. The shell wires them
— the features never know of each other.

**Check:** Gradle

---

---

### `mod-core-clean` — Core modules know no type of the app

`:next:core:model` is plain Kotlin without Android; `:next:core:common` has no Compose and holds
the texts; no core module knows a type of `:myExpenses` or of a data module.

**Why:** Core modules are shared across all features. Domain types or data-layer knowledge in core
would create coupling that makes the graph harder to understand and change.

**Check:** Gradle

---

---

### `mod-adapter-interfaces` — Features declare interfaces for what they need from the app

What a feature needs of the app it declares as an interface (e.g. `CategoryWriting`,
`TagDatabase`, `FixedTransactions`, `ReceiptDatabase`) or takes from a composition local of
`:next:core:ui` (`LocalAmountFormatter`, `LocalFontIcons`). The shell implements it in a file of
its own named after what it uses, e.g. `ProviderCategoryWriting`, `RepositoryTagDatabase`,
`AppFixedTransactions`.

**Why:** A feature that imports from `:myExpenses` directly cannot be built, tested or reviewed
without the whole app. Interfaces keep the dependency boundary explicit and let tests supply fakes.

**Check:** review

---

---

### `mod-own-packages` — Each module has packages of its own

Packages start with the module's namespace: `org.totschnig.myexpenses.next.core.model.ledger`,
`…next.data.contracts`, `…next.feature.contracts`. No two modules share a package. Only the shell
keeps `org.totschnig.myexpenses.next` and its sub-packages, since the launcher points at
`org.totschnig.myexpenses.next.MyExpensesNext`. The texts are
`org.totschnig.myexpenses.next.core.common.R`.

**Why:** Shared packages mean that `import org.totschnig.myexpenses.next.foo.Bar` could refer to
code in any module, making the dependency graph opaque. Own packages make imports self-documenting.

**Check:** Konsist

---

---

### `mod-data-headless` — Data modules have no UI

A data module `:next:data:*` holds the facts, analyses, stores and interfaces to the app of an
area, without Compose, so that widgets and notifications can use them without a screen. It depends
on core modules, on data modules of other areas without cycles and on the modules without app
types, never on a feature, `:next:core:ui`, `:designsystem`, `:charts`, `:ui_next` or
`:myExpenses`. The plugin `next.data` fails on such a dependency.

**Why:** A widget or a notification cannot host a screen. Data that is only accessible via Compose
cannot be shown there. Keeping Compose out of data modules also makes their logic faster to test.

**Check:** Gradle

---

---

### `mod-shell-boundary` — The shell knows an area only through its package

The shell knows an area only in its own package (`org.totschnig.myexpenses.next.<area>`, named like
the modules of the area): its entry `…Feature` with what it adds to the main screen, its settings
(`AreaSettings`) and the adapters to the app. Besides them only `NextFeatures`, which lists the
areas, and `NextData`, which creates their repositories, import `next.feature.*` or `next.data.*`.
The main screen, overview, list, theme, settings, the first start and `MyExpensesNext` show what
the entries give them ([`NextFeature`], [`AreaSettings`]), so that an area is added or left out
with its modules, its package and its lines in `NextFeatures` and `NextData`. Facts an area reads from an area that may be left out go
through one value of `NextData` with an empty value, e.g. `receiptFacts`.

**Why:** If the main screen imported feature classes directly, adding or removing a feature would
require changes throughout the shell. Routing everything through a package boundary and a feature
list makes the shell additive: one area = one package + two lines.

**Check:** Konsist

---


## Features and packages

An area, e.g. `contracts`, `tags`, `budgets`, has the modules `:next:data:<name>` (logic, package
`org.totschnig.myexpenses.next.data.<name>`) and `:next:feature:<name>` (screens, package
`org.totschnig.myexpenses.next.feature.<name>`).
What only works with the app stays in the shell `:ui_next`: settings, onboarding, search, the
reading of the ledger and the adapters of the features. The ledger, its facts and its view models
are described in
[`ledger/README.md`](../next/core/model/src/main/java/org/totschnig/myexpenses/next/core/model/ledger/README.md).

### `feat-read-data` — Features read the ledger and data modules, not each other's view models

A feature reads the ledger (`core.model.ledger`) and the facts of other areas from their data
modules, never their view models, screens or stores. Screens of another area are handed in by the
entry of the area in the shell (`mod-feature-isolated`, `mod-shell-boundary`).

**Why:** View models hold transient state and are tied to a lifecycle. Reading from them would
couple one feature's state machine to another's, making both harder to change.

**Check:** Gradle

---

---

### `feat-data-no-cycles` — No cycles between data modules

**Why:** Cycles make the build graph ill-defined and force all involved modules to be rebuilt
together. Breaking a cycle later is expensive. Forbidding them from the start keeps the graph a
DAG.

**Check:** Gradle

---

---

### `feat-single-source` — Each concept lives in one place

What only one area needs stays in its modules; its logic goes into its data module, even if only
its screens use it. What more than one feature needs about a transaction goes into
`LedgerTransaction` (e.g. whether it is sealed), not into a second query.

**Why:** Duplication is the main source of inconsistencies over time. One place means one fix.

**Check:** review

---

---

### `feat-no-duplicate` — No duplicate computations

Each concept lives in one place, e.g. the merchants in `Merchants`, the thresholds of a saving tip
in the tip, which the guidance reuses. Code that computes the same thing a second time is a
finding.

**Why:** Two implementations of the same logic diverge. A bug fixed in one is not fixed in the
other.

**Check:** review

---

---

### `feat-readme` — Features with many files have a README

A feature with more than a few files has a `README.md` that says what it does and how its parts
work together, with diagrams as `.puml` next to it, never ASCII art.

**Why:** A feature without a README forces the next reader to reconstruct its design from the code.
A short README and a diagram pay back the time spent on them many times over.

**Check:** review

---

---

### `feat-readme-sync` — The README matches the code

The README, the `.puml` files and the table of view models in `ledger/README.md` match the code:
names, what reads what, what is offered.

**Why:** A README that contradicts the code is worse than none: it misleads. Keeping it in sync is
part of the commit that changes the code.

**Check:** review

---

---

### `feat-root-shared` — Root packages hold only shared code

Files in the root package of `:next:core:ui` (`…next.core.ui`) are shared by the screens of
several features (formats, layout, theme), files in the root package of the shell (`…next`) arrange
them (router, main screen). Code of one area belongs into its modules.

**Why:** The root package of a shared module is the most visible place in it. Putting area-specific
code there hides its true home and makes the module harder to split later.

**Check:** review

---


## Analyses and facts

### `analysis-pure` — An analysis is a pure function of its input

An analysis is a pure function of an input data class, tested without Android, e.g.
`contractTransactions(ledger)`, `balanceTransactions(ledger, period)`.

**Why:** A pure function can be tested exhaustively on the JVM. A function that reads global state
can only be tested by setting up that state, which is slow and fragile.

**Check:** review

---

---

### `analysis-no-clock` — An analysis does not read the clock

An analysis reads nothing outside its input, not even the clock: the day comes from
`LedgerSource.today`, which changes at midnight and when the user sets the date, time or time zone,
so that a result of yesterday is not taken for today. `LocalDate.now()` is fine for what the user
does at a moment (e.g. the day a contract is cancelled) and for previews, not in an analysis.

**Why:** A function that reads `LocalDate.now()` returns a different result for the same input
depending on when it is called. That makes it impossible to test for a specific day and easy to
show stale results.

**Check:** review

---

---

### `analysis-data-class` — Inputs and facts are data classes

Inputs, facts, `Ledger` and `LedgerTransaction` are data classes, so that equal data compares
equal. The test checks classes named `…Input` and `…Facts`.

**Why:** The `Memo` (rule `analysis-memo`) skips a recomputation when the input has not changed.
That comparison only works correctly if equal data produces equal objects — which requires
`data class` and its generated `equals`.

**Check:** Konsist

---

---

### `analysis-memo` — Each analysis is computed with a `Memo` of its input

The repository or view model computes each analysis with a `Memo` of its input, and the input holds
only what the analysis reads.

**Why:** Without memoization, an analysis reruns every time any state changes — even if the input
did not change. Memoization avoids the work and prevents unnecessary recompositions.

**Check:** review

---

---

### `analysis-tested` — Each analysis has a test for when it recomputes

Each analysis has a test that an unchanged input is not computed again and that a relevant change
is, see `BalanceViewModelTest` and `ContractInputTest`.

**Why:** The `Memo` is only valuable if the input is defined correctly. A test that checks
re-computation catches an input that is too broad (reruns always) or too narrow (misses changes).

**Check:** review

---

---

### `analysis-shared-flow` — Facts other features read are shared flows

Facts that other features read are a shared flow with `replay = 1` (`shareWhileShown`). A fact that
is null means loading or no consent; screens turn it into an `AnalysisState` with
`analysisState(consent, value)`.

**Why:** A cold flow would restart its computation for each collector. A shared flow with replay
means the latest value is available immediately to a new collector (e.g. a screen that appears)
without restarting the analysis.

**Check:** review

---

---

### `analysis-pure-planner` — Work with side effects runs only on a new result, decided by a pure planner

Work with side effects, e.g. automatic decisions about contracts, runs only for a new result, and
is decided by a pure planner with tests (`AutomaticDecisions`).

**Why:** A planner that can be tested separately from the repository that applies its decisions is
easier to verify and change. Running side effects only on new results prevents re-applying a
decision after a restart.

**Check:** review

---

---

### `analysis-in-repo` — Facts are computed in repositories, not view models

Facts other features read, and what a widget or notification could show (balance, tips, pots), are
computed in a repository of the data module (`…Repository`), with the `CoroutineScope` it is given.
`NextData` in the shell creates the repositories once per process; view models read facts and the
home currency (`NextData.homeCurrency`, which follows a change by the user) from there, never from
another view model, so that the order in which `MyExpensesNext` creates them does not matter.

**Why:** A view model is tied to a screen's lifecycle. If facts live in a view model, they vanish
when the screen is off screen, and another feature cannot read them without importing the view
model. A repository lives for the process, independent of any screen.

**Check:** review

---

---

### `analysis-decisions-in-repo` — Automatic decisions are applied in repositories, not view models

Automatic decisions (decisions about contracts, the rules of tags, certain categories) are made in
the repository of their area as a shared flow with the side effect, never in a view model.
`NextData.applyAutomaticDecisions` collects them while `MyExpensesNext` is started and for a
minute after `TransactionImports` tells of an import from the bank or a file, so that nothing is
analysed in the background otherwise. A replayed result after a restart is not decided on again.

**Why:** A view model is destroyed when the user leaves the screen. Automatic decisions that run
there stop the moment the screen is dismissed. A repository that the shell drives keeps the
decisions running for the lifetime of the app, not the screen.

**Check:** review

---


## View models and stores

### `vm-constructor` — View models get their dependencies through the constructor

View models of the new UI are plain `ViewModel`s (or `AndroidViewModel` when they need the
application) with constructor parameters: their repository, or the `LedgerSource` and one parameter
per kind of facts, their store and what they take from `injector`. No field injection, no `by
lazy`, no `start()`. `injector.inject` is only used for view models of the main module.

**Why:** Constructor injection makes the dependencies explicit and testable. Field injection hides
them, and a `start()` method creates a window where the object is partially initialised — a source
of bugs.

**Check:** Konsist

---

---

### `vm-graph-owners` — Only `NextData` and the area entries own the dependency graph

`NextData` creates the repositories, the entry of each area in the shell (`…Feature`, listed in
`NextFeatures`) its view models with `viewModelsOf { … }`; together they are the only places that
know the whole graph.

**Why:** If many places create or look up view models and repositories, it becomes impossible to
see at a glance what the app creates and how the pieces fit together. Two owners — one per layer —
is the minimum that makes the graph readable.

**Check:** test

---

---

### `vm-stores` — User decisions live in stores, not view models

Decisions of the user (rules, pots, closed hints) are kept in a store of the feature (`…Store`),
not in the view model, and the store is tested with `withTestDataStore`. A view model does not read
or write the DataStore itself.

**Why:** A view model's state is lost when the screen leaves memory. A `DataStore` persists across
process restarts. Keeping user decisions in a store means they survive and are backed up; keeping
the view model unaware of the DataStore directly means the store can be tested and swapped
independently.

**Check:** review

---

---

### `vm-entry-points` — View models are imported only by entry points

View models are imported only by `MyExpensesNext`, `NextSettings`, the entries of the areas in the
shell (`…Feature`) and the entry composables (`…Flow`). A screen below them gets state and actions,
not the view model.

**Why:** A screen that imports a view model cannot be previewed, tested as a composable or reused
in a different context. Entry points own the view model; screens below them receive plain data.

**Check:** Konsist

---

---

### `vm-scope-for-effects` — Writes and network calls run in the scope of a view model or repository

Writes and network calls run in the scope of a view model or repository, never in
`rememberCoroutineScope`, so that a rotation or a closed dialog does not cancel them. The screen
gets the result as a callback; a composable scope is fine for reads, navigation and snackbars.

**Why:** `rememberCoroutineScope` is cancelled when the composable leaves the composition — which
happens on a rotation. A write that is cancelled halfway leaves the data in an inconsistent state.
The scope of a view model survives rotations.

**Check:** review

---

---

### `vm-saved-state` — Surviving state goes into the `SavedStateHandle`

What the user is entering and what waits for another activity (the document scanner, the licence
dialog) is kept in the `SavedStateHandle` that `viewModelsOf` hands to the view model, so that it
survives when Android ends the process. A callback kept in a field of the activity or a state held
only in memory is lost then.

**Why:** Android can end the process of a backgrounded app at any moment. The `SavedStateHandle`
is serialised and restored by the OS. State that is only in memory is gone after such a kill, and
the user has to start over.

**Check:** review

---

---

### `vm-io-param` — A view model that does IO takes its dispatcher as a parameter

A view model that reads the database, files or the network takes its dispatcher as a parameter
(`io: CoroutineDispatcher = Dispatchers.IO`) and what it needs of Android as interfaces or
functions, e.g. `ItemGroupModels` or a photo loader, so that it is a plain `ViewModel` that tests
run in the JVM, see `ReceiptViewModelTest`.

**Why:** A view model that calls `Dispatchers.IO` directly cannot be tested without a coroutine
test dispatcher. Taking it as a parameter lets tests inject `UnconfinedTestDispatcher` and run
without emulator.

**Check:** Konsist

---


## Screens

### `screen-single-entry` — Each feature opens through one entry composable

Each feature opens through one entry composable (`…Flow`). The entry of its area in the shell
(`…Feature`) puts it on a page of the back stack (`pages`, one `NextKey` each) and brings its tab;
`NextMainScreen` only arranges the tabs, pages and overview parts of the features listed in
`NextFeatures`.

**Why:** A single entry point is the only place where a feature's view model is created and its
state is initialised. Multiple entry points would duplicate that setup and make it hard to reason
about the feature's lifecycle.

**Check:** review

---

---

### `screen-actions-interface` — One actions interface per feature

What a user can do on the screens of a feature is one interface (`…Actions`), implemented by the
view model, rather than a callback per action.

**Why:** A callback per action scales poorly: a screen with ten actions gets ten lambdas in its
signature. An interface groups them, makes the contract explicit and allows the view model to be
passed to previews as a fake implementation.

**Check:** review

---

---

### `screen-from-ds` — Screens are built from the design system

Screens are built from the components of `:designsystem`. A pattern it does not have, or one that
appears twice, is added there with an entry in `Showcase.kt`, not built by hand in a feature.

**Why:** A pattern built by hand in a feature will look slightly different from the same pattern in
another feature. Adding it to the design system means one implementation, one look, one place to
fix.

**Check:** review

---

---

### `screen-no-alert-dialog` — No `AlertDialog` in `:ui_next`

Dialogs come from the dialogs of the design system.

**Why:** Material 3's `AlertDialog` and the design system's dialogs have different styling. Using
the raw component bypasses the design system and creates inconsistencies.

**Check:** Konsist

---

---

### `screen-pure-ui` — Composables hold no business logic

Composables show state and call actions. A computation longer than a few lines belongs into a pure
function of the feature, which can be tested.

**Why:** Logic in a composable is hard to test without a UI test and impossible to test without
Android. Moving it into a pure function means a JVM test can cover it.

**Check:** review

---

---

### `screen-strings-next` — Texts of the new UI are string resources prefixed `next_`

Texts of the new UI are string resources of `:next:core:common` in English and German, prefixed
`next_`. In the UI a tag is called "Gruppe", the area of a contract "Bereich".

**Why:** The prefix separates new-UI strings from the existing ones in `:myExpenses`, preventing
name collisions and making it easy to find and translate all new strings.

**Check:** review

---

---

### `screen-use-formats` — Dates and amounts are formatted with `Formats.kt`

Dates and amounts are formatted with the helpers of `next/Formats.kt`, not with formatters created
ad hoc.

**Why:** Ad-hoc formatters use different locales, date patterns and rounding. A shared helper
formats consistently across all screens.

**Check:** review

---

---

### `screen-nav3-stack` — Each feature uses a Navigation 3 back stack

The screens a feature opens within itself (details, lists below it) are entries of a Navigation 3
back stack, one `@Serializable` key per screen in a sealed `…Key`, so that back and predictive
back return to where a screen was opened from. No flags with `BackHandler` for that; dialogs and
edit modes stay local state. A page whose content is looked up in the state of the feature leaves
itself with `shownOrLeave` once its content is gone. Done in `contracts`, `budgets`, `analysis`,
`balance`, `tips` and the settings (`SettingsKey`, the pages behind entries as
`SettingControl.Page`). The shell has one too (`NextKey`): the tabs, the budget and the balance are
pages on top of the overview, a jump between features, e.g. from a saving tip to a contract, puts a
page on top, and a feature gets where to start, e.g. the contract to show, as part of the key
instead of a flag it has to reset.

**Why:** A `BackHandler` flag approach requires the feature to manage its own back-stack manually,
which diverges per feature. Navigation 3's back stack gives correct predictive back animations and
serialisable state for free.

**Check:** review

---

---

### `screen-dialog-vs-sheet` — Dialogs for one decision, sheets for choice or form

A dialog asks for one decision or one short value: confirming, a name or text, a date or time, a
short single choice. A choice of several, a filter, a form about something that stays visible below
(e.g. an item of a receipt), the details of a transaction and the actions of something are sheets
of the design system (`AdaptiveSheet`, `MultiChoiceSheet`, `FilterSheet`, `FormSheet`,
`ActionSheet`). In wide windows they come in from the side. No `ModalBottomSheet` outside
`:designsystem`.

**Why:** The distinction between dialog and sheet maps to what the user is doing: a dialog demands
an answer before anything else; a sheet is a panel that can coexist with the screen below. Using
the design system's sheets also gives the correct adaptive behaviour in wide windows for free.

**Check:** review

---

---

### `screen-adaptive` — Wide windows show list and detail side by side

Wide windows (`isWideWindow`, tablets and phones in landscape) show the same new design:
`NextMainScreen` puts the tabs in a rail instead of the bar, and a feature whose start page is a
list (`contracts`, `budgets`, `tips`, the settings) puts it and the page opened from it side by
side with `rememberListDetailPages` and `ListPage`/`DetailPage`/`ExtraPage` on the entries of its
back stack (rule `screen-nav3-stack`). A screen with two parts that belong together, e.g. the photo
of a receipt and its rows, may show them side by side there.

**Why:** A bottom navigation bar and a full-screen list waste space on a tablet. The list-detail
layout is the standard adaptive pattern for wide screens and is expected by users of those devices.

**Check:** review

---


## Data of the app

### `data-bank-import` — The new UI assumes transactions come from a bank import

The new UI assumes that transactions come from a bank import (FinTS or CSV). It does not port plans
or the calendar, and contracts never create transactions.

**Why:** The import-first model simplifies the transaction flow. Plans and calendar entries are a
separate mental model; porting them would add complexity without serving the core use case that the
new UI is designed around.

**Check:** review

---

---

### `data-backup` — User-entered data is backed up; downloaded data is not

Data the user enters (rules, pots, closed hints) is part of the backup. Downloaded or derived data
(logos, models) lies in `noBackupFilesDir` and can be loaded again.

**Why:** Backing up downloaded data wastes backup quota and slows the backup. Data the user entered
would be lost without a backup; data that can be re-downloaded would not.

**Check:** review

---

---

### `data-consent` — Nothing leaves the device without explicit user consent

Nothing leaves the device without a consent the user gave for it, and only what the consent names
(e.g. the domain of a merchant, never amounts).

**Why:** The app handles financial data. Users must be able to trust that it does not send their
data anywhere without their knowledge. Consent must be specific: agreeing to logo lookup does not
cover sending amounts.

**Check:** review

---


## Changing a rule

A rule that no longer fits is changed here, in the same commit as the code, with a sentence why in
the commit message. A finding that is accepted as an exception gets a comment in the code naming
the rule tag, e.g. `// Exception to analysis-no-clock: …`.
