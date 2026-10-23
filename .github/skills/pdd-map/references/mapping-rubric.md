# Mapping rubric

Every format rule — Actors and roles, Interfaces and their division into
Experiences, Screens, Domains, naming, Entities, the Capability split test,
Business Rules and prohibitions, Journeys, Step and Screen facts, availability
in every context, Variations and what selects them — lives in
[format.md](format.md). This rubric holds only the mapping method: how to find
those resources in a repository and where the evidence stops.

## Inspect by behavior

- Start at user and operator entry points, then trace handlers or services,
  persistence or external effects, and observable outcomes.
- Read configuration, authorization, telemetry, jobs, and tests when they
  materially change product behavior.
- Use documentation as a lead. Confirm current claims in implementation.
- Never execute target code and never claim deployed or live state from source.
- Repository deployables, routes, commands, APIs and integrations are evidence,
  not automatic Interfaces. Map an Interface only for a supported Product
  interaction contract.
- Treat shared backend code as no evidence of web/mobile/API/CLI parity. Verify
  each declared availability Context independently; offering a Capability on
  two Interfaces because one implementation serves both is a question for the
  author, not a default.
- Read each Interface's access from who reaches its places in the code — which
  routes or commands work without signing in, which need a session, which only
  the roles that administer the Product, its settings or members, may enter
  (the administration area). `lint` cannot see this condition until Experiences
  exist; deciding it is yours.
- Read permissions from the authorization checks the code performs, never from
  names. A privilege that exists only in code is not product meaning.
- Read `languages` from the locales the code ships, per Product and per
  Interface. Content kept in several languages and a person's chosen language
  are Entity facts (format reference), never `languages`.

## Acceptance coverage

Capability Scenarios state observable acceptance for one Capability. Cover the
primary path, refusals of who may act, invalid input, conflicts, and external
failures only where the Product distinguishes them. These are cases, not kinds:
record each under a `kind` that `taxonomies.yaml` declares — a refusal of who
may act is `validation`, a conflict or an unavailable dependency is `edge`. Where the line falls between a Scenario
and an `## Edge cases` bullet is the author's call and belongs in the Acceptance
round.

## Entity granularity

Apply the Entity test in the format reference: write `## Information kept`
first and read the count off it. Put both shapes and their counts to the
author when you can; with no author to ask, split and surface the unresolved
granularity question in the proposed delta. Once resolved, record the resulting
meaning without retaining the compared shapes.

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

## Describe coverage

Fill `coverage.md` as the format reference shapes it: which of the repository's
code the model accounts for, at the highest level that still guides an agent —
the folders whose behavior the model describes, the code deliberately outside
it because it is not product behavior, code holding behavior not yet modeled,
and code whose behavior could not be established. Every entry names its paths;
never map file by file, and never state what the Product is or is not here.
Coverage never states whether behavior is implemented or verified. A small,
honest model with recorded gaps is better than a broad model built from
guesses.

## Use References honestly

Attach to each resource what you actually read to establish its meaning:
`kind: code` with `role: implementation` for the code you traced,
`role: intent` for the spec, PRD or proposal that states the behavior, and
`role: context` for background you read. For code targets, prefer
`path#symbol` over line ranges and use only tracked files. A Reference records
where a claim came from, never that it is verified, and none is required — but
a resource with nothing attached rests on inspection alone: justify it from
that inspection and say so in the proposed delta.

Visual or research References may guide inspection. Keep their role honest,
never treat their existence as proof, and never run screenshot capture
workflows.
