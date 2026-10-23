# Planning rubric

Every format rule — Actors and roles, Interfaces and their division into
Experiences, Screens, Domains, naming, Entities, the Capability split test,
Business Rules and prohibitions, Journeys, Step and Screen facts, availability
in every context, Variations and what selects them — lives in
[format.md](format.md). This rubric holds only the planning method.

## Scope

- Plan one coherent intent a reviewer can approve or reject as a whole.
- Prefer the smallest product-complete change over a speculative epic.
- In a verification handoff, solve the exact gap; do not broaden the product.

## Decide each commitment

- Decide web, mobile, CLI, partner API, and integration commitments
  independently; internal APIs and frameworks are not Product Interfaces.
- Do not assume parity across Interfaces. Decide each availability Context and
  every Scenario's Step Contexts independently.
- Decide each Interface's access from who will reach each planned place —
  without signing in, once signed in, or only the roles that administer the
  Product, its settings or members, may enter (the administration area). `lint`
  cannot see this until Experiences exist; deciding it is yours.
- Record intent as the outcome a boundary or behavior protects, without
  comparisons to discarded designs.

## Places, not designs

The model records what an Actor can reach, see, supply, do and trigger at each
place. It never says how that looks or is built. One test decides every case:

> Rebuild the surface from scratch: a web or mobile view with a different
> component library, layout, typography, colors, spacing, icons, motion and
> copy; a CLI with a different command style; an API in a different style, CRUD
> or RPC. Everything that would still have to be true is the model's: who can
> reach the place, what facts it shows, what abilities it offers, what
> conditions change that, and what happens next. Everything the rebuild is free
> to change is design's, and the model says nothing about it.

Design — component libraries, theming, layout, typography, color,
iconography, motion, microcopy and tone, gestures versus buttons, modal versus
page, tabs versus one page, a wizard versus one form, breakpoints, loading and
hover states, menus and what they contain, quality attributes such as
accessibility or performance, a CLI's command syntax, flags and output format,
and an API's style (CRUD or RPC), paths and payloads — lives in `visual`,
`doc` or `spec` References with `role: intent`, never in prose. Ordinary copy is design; when exact wording is
a product requirement, a Business Rule identifies its authoritative Reference.
A quality the Product commits to whatever the design — a stated accessibility
conformance level, a contractual response time — is a Business Rule that names
its authoritative Reference the same way; how a design meets it stays design.

## Scenarios are the acceptance contract

Every Capability needs at least one Capability Scenario; cover the primary
path, refusals of who may act, invalid input, conflicts, and external failures
where the Product distinguishes them. These are cases, not kinds: a new model
declares exactly `primary`, `edge` and `validation`, and records each case
under one of them, never a new kind per case — a
refusal of who may act is `validation`, a conflict or an unavailable dependency
is `edge`. Write Trigger, ordered typed Steps, Decision points when a linear
sequence branches, and Outcome so a reviewer can compare source behavior
without executing it.

- Good: “Submitting an empty cart shows an error and keeps the cart.”
- Too vague: “Cart validation works.”
- Wrong altitude: “POST /cart returns 400.”

## Check the whole model before approval

Walk these on the proposed delta before presenting it, and again once `lint`
is clean; lint cannot see them.

- **Lifecycle:** every Entity a person creates can be changed and removed by a
  Capability, or a Product limitation says it is not ("Links are never
  deleted; disabling one stops its redirect"). Renaming is changing.
- **Every Journey:** each Capability Scenario whose last Product Step lands the
  same Actor where they use another Capability is a Journey; none other is.
- **Opposite verbs:** no Capability hides an opposite verb in a Scenario;
  each control's verb has its own Capability.
- **Who may act:** every create, change and remove — a person's, an agent's,
  and the Product's own Steps inside their runs — is selected by a permission
  Rule whose grant admits that Step. A cascade is marked, not granted: a
  creation or removal that goes with another of the same Step says `with`
  (creating a board makes its first membership in that same Step), and only
  the one it goes with needs permission. A grant about one's own thing reaches the
  person through `related`, never a bare `actors` list that means everyone with
  that role.
- **Invariants:** a Rule about a thing's facts or States targets the Entity.
- **AI:** one of the two shapes in format.md, never an assistant that acts.
- **What the Product leaves to other systems:** say it once as a
  product-language limitation ("People sign in with their existing account;
  managing accounts is not part of this product."), never in coverage.

## Dialogue

- Propose concrete drafts and let the user correct them.
- Batch related open questions; ask only decisions the user must make.
- While a choice remains open, discuss a recommendation and its tradeoff in the
  conversation. After resolution, record the resulting product meaning without
  retaining discarded directions or replaying settled discussion on later runs.
- Resolve material points before approval instead of guessing; one still open
  is stated in the prose of the resource it affects, never in coverage. An
  unchosen option is not a limitation or product exclusion.
- Keep screenshots, mockups, design systems, research, and sitemaps external.
  References may attach them with `role: intent` or `role: context`, but
  pdd-kit neither creates nor certifies them.
