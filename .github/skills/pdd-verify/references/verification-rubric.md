# Verification rubric

Every format rule — what each resource claims, how Interfaces divide into
Experiences, the Capability split test, Business Rules, Journeys, Step and
Screen facts, Variations and what selects them — lives in
[format.md](format.md). This rubric holds only the verification method.

## Trace behavior

Compare the model's observable contract, not matching vocabulary. Tests
corroborate source; they do not replace inspecting implementation. Partial
implementation is a gap, not alignment. Never claim deployed configuration,
external systems, or live data state from source code.

- **Routes.** Trace each Capability Scenario route through every typed Step and
  most-specific Context place to its observable outcome. Trace every Journey
  Scenario route from its first Actor-owned Context place through each
  Capability-bearing Step to the terminal goal result, and compare the one
  authored Steps claim directly with repository behavior. Confirm every Step
  naming an actor is supported at its Context places and every derived
  availability place supports a Scenario Actor.
- **Interfaces.** Verify each availability Context independently; shared
  services do not prove web, mobile, CLI, or API parity. Distinguish a missing
  Interface commitment from a missing shared Capability. Undeclared internal
  APIs remain implementation detail. Confirm each Interface's division against
  who actually reaches its places — `lint` cannot see access until Experiences
  exist.
- **Nouns.** Confirm the Product keeps each named fact `## Information kept`
  claims, that each named state is one the Product distinguishes rather than an
  implementation flag, and that each declared relation and its cardinality
  hold. For every Step's `entities`, confirm the code performs each declared
  effect, moves the thing between exactly the states named, touches nothing the
  Step leaves out (apart from the log entry a recording Rule makes, checked
  under **What is recorded**), and reads, changes or initializes exactly the `facts` it
  cites.
- **Screens.** Compare each place against its view's code: the facts disclosed
  match `shows` and the inputs match `collects`; each derived Capability is one
  a Step placed there uses; each Screen is one working context, its tabs and
  wizard stages included. Never require component, layout, theme, menu,
  ordinary copy, viewport, or screenshot similarity.
- **Languages.** Confirm the Product's `languages` against the locales the code
  serves, and each Interface's list against what that surface loads.
- **Who may.** For each Business Rule with `permits`, confirm the code lets
  exactly the granted actors perform the operation, under the stated
  conditions, and refuses everyone else; an operation closed with
  `permits: []` must be refused. A grant the code does not enforce is reported
  as **not established**. Confirm a fact-scoped Rule — a derivation, a field's
  visibility or edit — against the code that computes, shows or writes the
  fact, at every Screen presenting it and every Step citing it, never Entity
  presence alone. A condition naming another Entity reads the one instance the
  model fixes: confirm the code checks that instance — where two to-one walks
  reach it, that they meet at one — and that a `singleton` is kept once. Steps
  support takes while acting as a customer must be refused without the consent
  a grant requires.
- **What is recorded.** For each Rule that says a log Entity records
  operations, confirm the code writes an entry, naming who acted, for every
  Step the Rule selects and for nothing it leaves out.
- **What varies.** Trace each alternative under its own `selectedWhen`: the
  code supports it now, reads the named setting, assignment or discriminator to
  choose it, applies the stated default for a missing or unsupported choice,
  and re-reads and keeps the choice as `takesEffect` and `stability` say. An
  alternative the code no longer offers, a choice made by something other than
  what the set names, or an undocumented default is a finding. Hold an
  alternative permission Rule's grants against the Steps and Screens that run
  under that alternative only — `lint` cannot. Where a resource's lead says it
  exists only under some alternatives or only while a setting, plan or licence
  enables it, confirm that is where the code offers it.
- **Wording.** External visuals and research are context; their existence is
  never proof. Inspect an authoritative text Reference when a Rule requires
  exact wording; if it cannot be accessed, report that requirement as
  unverifiable.

## The border

The model records what an Actor can reach, see, supply, do and trigger at each
place. It never says how that looks or is built. The redesign test decides
which side a difference falls on:

> Rebuild the surface from scratch: a web or mobile view with a different
> component library, layout, typography, colors, spacing, icons, motion and
> copy; a CLI with a different command style; an API in a different style, CRUD
> or RPC. Everything that would still have to be true is the model's: who can
> reach the place, what facts it shows, what abilities it offers, what
> conditions change that, and what happens next. Everything the rebuild is free
> to change is design's, and the model says nothing about it.

A difference the redesign is free to make is never a finding: component
libraries, theming, layout, typography, color, iconography, motion, copy,
gestures, modal versus page, tabs versus one page, a wizard versus one form,
breakpoints, loading and hover states, menus and what they contain, a CLI's
command syntax, flags and output format, and an API's style (CRUD or RPC),
paths and payloads belong in `visual`, `doc` or `spec` References with
`role: intent`. Ordinary copy is design; compare exact
wording, or a committed quality such as an accessibility conformance level,
only when a Business Rule requires its authoritative Reference, and report it
unverifiable when that source is unavailable.

## Separate scope from authority

Git diffs identify likely changed surfaces. They never establish a plan, choose
truth, or prove that an unchanged file is irrelevant. Include unchanged
dependencies when they determine scoped behavior.

When authority is not already explicit, present:

1. what the model says;
2. what code currently does;
3. exact inspected files;
4. the smallest meaningful choices;
5. a recommendation and why;
6. downstream findings the answer resolves.

Group questions by root decision. Do not ask a menu of symptoms.

Discuss the choices only while authority remains open. After resolution, report
the resulting decision without replaying settled alternatives on later runs.

## Internal intent resolution

Use when code-right or neither-right is chosen. Draft the smallest exact Product
Model delta, covering affected Interfaces, optional Experiences, Capabilities,
availability Contexts, Rules, Capability Scenarios, Journeys, Journey
Scenarios, relationships, and removals. Get approval before writing. Skip broad
brainstorming because the verification finding already supplies the problem.

## Internal scoped mapping

Use only for established behavior in an absent or deliberately untrusted model
area. Do not silently remap trusted areas. Inspect it like adoption mapping:
start at entry points, trace handlers, persistence and outcomes, confirm docs
in implementation, read permissions from authorization checks rather than
names, and read each Interface's access from who reaches its places. Draft
every resource by the format reference, apply the border above, draft honest
coverage and necessary relationships, and get approval before writing.

## Slices and phases

Phases apply only when implementing with verify, which the user opts into. A
slice is the unit verify checks and reports in either mode: what one
inspection can confirm.

- One Capability with its Capability Scenarios, the Entity facts and states its
  Steps touch, the Screens placing it, and the Business Rules targeting it.
- A Journey is its own slice, after every Capability it carries the Actor
  through; it builds only the hand-offs between them.
- An Interface or Experience is built with the first slice placed there. A
  Variation goes with each slice it varies; its default alternative first.

Order slices so that:

- whatever an Actor must reach first, such as access or setup, comes first;
- the slice whose Step creates a thing precedes every slice whose Steps read,
  change or move it;
- otherwise, by Domain id, then by resource id, so two runs agree.

A phase is the unit handed to the builder by default: the slices whose needs
are already met.

- The first phase holds the shared groundwork no slice owns, such as storage,
  access and the application shell, together with every slice that needs
  nothing another slice creates.
- Each next phase holds the slices whose needs the earlier phases met. A
  Journey joins the phase after its last Capability.
- Within a phase, slices keep the order above.

The pace changes only how much is handed over at once: one phase (the
default), one slice when the user asks for one at a time, or the whole plan
when the user asks for it in one go. Checking stays per slice at every pace.

## Stop safely

Beyond the stops in the skill's steps 7–9 (the user implements elsewhere, an
unchanged build-directed gap, unverifiable evidence):

- Product authority remains undecided: wait for that decision.
- Structural blocker prevents model comparison: report the lint finding first.
