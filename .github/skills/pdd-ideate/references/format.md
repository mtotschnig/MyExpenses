# Product Model format

Author current, approved product meaning. Never persist alternatives considered,
rejected approaches, why another option was not selected, or deliberation
history anywhere in `product-model/` — resource prose, supporting sections,
limitations, README or additional files; keep that discussion in the
conversation and do not replay it on later runs. An unchosen option is not a
product exclusion: record exclusions only when established or approved.
Preserve observable refusal and failure behavior, current constraints, and
material unresolved questions or missing evidence. Lint does not check prose
for deliberation history.

## Layout

A representative model looks like this:

```text
product-model/
├── README.md
├── config.yaml
├── taxonomies.yaml
├── coverage.md
├── .gitignore
├── product.md                    # or product/product.md beside logo.svg (and an optional cover.webp)
├── interfaces/<id>/
│   ├── interface.md
│   ├── screens/<id>.md                       # undivided, or shared by every Experience
│   ├── screens/<id>/screen.md               # expanded: holds assets, never screens/
│   └── experiences/<id>/
│       ├── experience.md
│       └── screens/<id>.md
├── domains/<id>.md                          # optional collection
├── entities/<id>.md                         # the things, including those that act
├── capabilities/<id>/
│   ├── capability.md
│   └── scenarios/<id>.md
├── journeys/<id>/                           # optional collection
│   ├── journey.md
│   └── scenarios/<id>.md
├── business-rules/<id>.md                   # optional collection
└── variations/<id>.md                       # optional collection
```

Use `<id>.md` while a resource has no assets or child resources. When it gains the
first one, move it to `<id>/<type>.md` and keep owned assets beside that file.
Put generated implementation captures under `implementation/`. The compact and
expanded forms never coexist and derive the same id. Optional `assets:`
frontmatter annotates existing files with `title`; it never creates or
classifies an asset, and there is no `state` key: a screenshot of a state
attaches to the Scenario or Edge case that reaches that state.

Use these exact compact and expanded paths:

| Resource type | Compact | Expanded | Typed children |
| --- | --- | --- | --- |
| Product | `product.md` | `product/product.md` beside `logo.svg` | — |
| Interface | `interfaces/<id>.md` | `interfaces/<id>/interface.md` | `screens/`, `experiences/`, or both |
| Experience | `interfaces/<interface-id>/experiences/<id>.md` | `interfaces/<interface-id>/experiences/<id>/experience.md` | `screens/` |
| Screen | `<screen-parent>/screens/<id>.md` | `<screen-parent>/screens/<id>/screen.md` | — |
| Domain | `domains/<id>.md` | `domains/<id>/domain.md` | — |
| Entity | `entities/<id>.md` | `entities/<id>/entity.md` | — |
| Capability | `capabilities/<id>.md` | `capabilities/<id>/capability.md` | `scenarios/` |
| Capability Scenario | `capabilities/<capability-id>/scenarios/<id>.md` | `capabilities/<capability-id>/scenarios/<id>/capability-scenario.md` | — |
| Journey | `journeys/<id>.md` | `journeys/<id>/journey.md` | `scenarios/` |
| Journey Scenario | `journeys/<journey-id>/scenarios/<id>.md` | `journeys/<journey-id>/scenarios/<id>/journey-scenario.md` | — |
| Business Rule | `business-rules/<id>.md` | `business-rules/<id>/business-rule.md` | — |
| Variation | `variations/<id>.md` | `variations/<id>/variation.md` | — |

Here `<screen-parent>` is the Interface or Experience folder that contains the
Screen. Screens never nest: `screens/` under a Screen is a `lint` error.

There is no `actors/` collection: **an Actor is an Entity that `acts`**, and
the word names the role such an Entity plays on a Step, an Interface, an
Experience, a Journey, or a Business Rule grant. Actors differ by Product
goals, triggers, responsibilities or privileges; two roles with the same goals
and permissions are one Entity. A product's fixed, shipped set of roles (owner,
administrator, member, guest) is a closed vocabulary: one Entity that acts per
role, named in grants' `actors`, never one person Entity with a Role fact —
even where configuration also defines roles or assigns people to them. Roles
people create at runtime are instances: one `Role` Entity with its own
lifecycle and a Capability that assigns it. Roles operators define in
configuration, outside the product, are one Role Entity (its name, permissions
and members) granted through `configuredBy`, with no Capability that creates
it; a configuration that also grants directly to people stays its own Entity,
and the people who hold configured roles are one acting Entity. A role held per
container, such as an organization or a project, is held through a membership
Entity that does not act, whose Role fact names the role. Such a role
administers its container, not the Product: the container's settings and
member pages stay `authenticated`, with the Role in a grant's `when`. A
condition's `entity` reads one instance, the first that applies: the acting
Entity itself (the one Entity `actors` names, or where `related` ends — a grant
listing several actors fixes none, so a condition on one of them is its own
grant); on the grant's `related` path, the instance the path reaches
(nearest the acting Entity where the type repeats), so the Role read is the
acting person's own membership; the one the targeted instance has, walking
relations where every hop leaves exactly one (from the `many` side of a
`one-to-many`, either way along a `one-to-one`; never `many-to-many` or a
self-relation) — the Quiz a Question is in, the Workspace a Document belongs
to; where two walks through different Entities reach it (a suggestion's Habit
and its Reflection each have an Owner), the model claims they meet at one
instance and `verify` checks it, while two relations joining the same pair (an
Account that sends and receives Transfers) never meet and read nothing; or a
`singleton: true` Entity, the Product's one settings instance. A condition on
the governed thing itself names no `entity`. Any other `entity` is a `lint`
error: walk to it with `related`, relate it to-one,
or mark it `singleton` when the Product keeps exactly one. Settings each
Workspace keeps are never `singleton`. Support staff acting in a customer's
account, logged in as them or through access the customer grants, are still
support: their Steps name the support Entity as `actor` at the customer's own
places (whose `actors` include it; no Experience of its own), entering and
leaving the account is not a Capability of its own, the customer's consent is
their own setting, turned on and off like any other (opposite verbs and all),
and each Rule support may pass there
gains a grant for it, conditioned on the customer's consent when the Product
asks for it (`{ actors: [support-agent], when: [{ entity: shopper, fact: Support access, is: On }] }`,
the targeted Order's own Shopper). `restricted` is only the Product's own
administration area. A relation that holds
whatever role a person has (the sender of a message) is declared to each role
Entity that can hold it, and ownership is one `related` grant per role. Facts
that belong to the person whatever their role — email, display and
notification preferences — live once on an Account Entity that does not act,
to which each role Entity relates one-to-one. Roles that share an Account are
one audience, and a Step any of them may take names the least privileged role
the Product's default permissions allow, ignoring grants later made on a single
resource.

IDs are lowercase kebab-case segments. Behavior-hierarchy and cross-cutting ids
are the bare file or folder name. Qualified ids for Interfaces, Experiences,
and Screens carry their path joined by `::` —
`reader-web::personal-library::unread-library` — because Experience and Screen
names repeat across Interfaces on purpose. Containment is by id prefix: a Step
on a Screen is inside its Experience and Interface, and a Rule selector on
either covers it. Two resources of the same kind sharing a path
suffix below their Interface are counterparts: the same thing on two
Interfaces, each listing its own facts and Capabilities.

Interfaces, Experiences, and Screens share one place-id namespace: every id
names exactly one place. Never give an Experience and a shared Screen under the
same Interface the same name — both would resolve to `interface-id::name`;
rename one. `lint` names both colliding files.

The path owns every parent relation. An Experience never writes `interfaces:`,
a Capability Scenario never writes `capability:`, a Journey Scenario never
writes `journey:`, and a Screen never writes `availability:`. Capability
Scenario and Journey Scenario IDs share one global namespace. Only `product.md`
declares `id:`. The first and only H1 is the title. Most resources use lead
prose as their description; Journeys and both Scenario types instead use
required named sections and must not contain lead prose. Put relations and
entry points in frontmatter and Product meaning in prose. Product tags and every
relation ID list contain unique values. Each recognized H2 appears at most
once; unrecognized H2 sections are preserved as structured supporting content.
Lead and section-body fragments do not contain another H1 or H2.

## Required shapes

- `config.yaml`: exactly `schema: 11` and `sdd.paths`.
- `product.md`: `id`, optional `summary`, `category`, `tags`, `authors`,
  `license`, `limitations`, `languages`, H1, lead description, and optional
  `## Intent`. `id` is lowercase kebab-case of at most 64 characters.
  `summary` is one line of at most 400 characters, `category` is lowercase
  kebab-case, `authors` are `{ name, url? }` records, and `license` is an SPDX
  identifier. Report hosts read them as portable Product identity and
  attribution. A public Blueprint requires `category`, at least one tag, at
  least one author, `license` and `product/logo.svg`; `summary` stays optional.
  `limitations` are deliberate constraints of the Product, written as product
  facts ("Comments are never edited"), including what it leaves to other
  systems ("People sign in with their existing account"); a sentence about the
  model itself — "not modelled", "outside the model" — is never a limitation.
  `languages` is a unique list of language tags the Product serves — `en`,
  `de-DE`, `pt-BR`, each matching `^[a-z]{2,3}(?:-[A-Za-z0-9]{2,8})*$`; an
  Interface may narrow it, and Experiences and Screens never carry it. Content
  kept in several languages is an Entity fact, and how a language is chosen for
  someone is a kept fact (*Preferred language*) with the Steps that set it;
  neither is `languages` or a Variation.
- `taxonomies.yaml`: `scenarioKinds` entries with `id`, `name`, `description`,
  and optional `colorSlot`.
- Interface: a supported, inbound Product interaction contract — customer web,
  operator CLI, partner API — never every deployable, route or internal API,
  and never inferred from technology or shared implementation; an inbound
  webhook or callback qualifies and makes its caller an Actor, and a different
  address or header alone never makes another Interface. Required `type`
  (`web|mobile-app|desktop-app|cli|api|webhook|messaging|voice|device|agent`)
  matching the contract; at least one `actors` entry naming an Entity that
  `acts` — **who uses** the Interface, a descriptive list, never a permission
  claim; optional Product-facing `entryPoints`, a list of `- <key>: <route>`
  (public routes and deep links, never internal navigation identifiers), each
  keyed by this Interface's own type or by another Interface's id when a reader
  arrives from that surface;
  optional `languages`, a subset of the Product's (listing any while the
  Product declares none is an error); H1, lead description, and optional
  `## Intent`. `## Capability boundary` is an error: `availability` on
  Capabilities already says what an Interface offers. A model with no
  Interface is an error. Nothing about
  navigation is authored — menus, what they hold, sitemaps, back links: Steps
  say movement and entry points say arrival. An outbound
  connection the Product opens is not an Interface: model it in the calling
  Capability, give that Capability an availability Context for where the Actor
  observes the result, and make its failure a Capability Scenario.
- Experience: who is there and what they can do inside its one Interface —
  never what it looks like, and never a page, command group or route tree.
  Non-empty `actors` supported by the owning Interface, whose union across its
  Experiences equals the Interface's Actors; required `access`
  (`public|authenticated|restricted`), the most open the context can be, read
  from who is there (see **Two conditions decide whether an Interface is
  divided** below). A setting that closes it is a grant's `when` on the
  operations it restricts, never a reason for an Experience of its own, and so
  is a setting that opens it (anonymous access). Optional Interface-keyed
  `entryPoints`; H1, lead and optional `## Intent`;
  `## Capability boundary` is an error. Whether an Interface holds Experiences
  is decided by those same two conditions. A Variation may span Interfaces
  without changing ownership.
- Capability: a durable Product ability — the smallest behavior that remains
  independently meaningful, never a UI label, Journey title or sequence step;
  its Scenarios vary conditions, route or result and never hide
  sub-capabilities. At least one `availability` Context — every Experience in
  which one of its Actors uses it (guests in a public one, signed-in Users in an
  authenticated one), never only the most open, each with its own counterpart
  Screen unless every Experience shares it; a Screen reached before and after
  signing in exists in each Experience that reaches it. Optional singular
  `domain`; H1 and lead description. **It declares nothing about Entities** —
  what it changes is what its Scenarios' Steps say, and a file still carrying
  `entities` is refused. Every availability Context needs a Capability
  Scenario: a gap is always an error.

  **The split test is the contract.** Parts of an ability are separate
  Capabilities when they differ in who may do them (a permission of their own:
  a separate grant), where they are offered (availability), or in verb —
  create, configure, archive and delete are four. Nothing else splits one:
  - not a different Actor — one Capability is offered in every Experience it
    is used from;
  - not a Business Rule that only constrains one part, such as a time limit;
  - not which grant applies, told apart by a fact of the thing acted on (a
    channel's privacy, whether a message is the Actor's own) — that is a
    grant's `when`;
  - not the Entity a way creates or the State it starts from — adding an
    authenticator app or backup codes, restoring from the archive or the
    trash, are Scenarios of one;
  - not filtering, sorting or searching within a browsing ability;
  - not a setting or the deployment: the ways it selects between stay in one
    Capability, as Scenario alternatives, separate Scenarios or decision points
    by **What selects** under Variations, and a way it adds beside the others,
    the Actor choosing among them, is an ordinary Scenario.

  The Steps of one run are one Capability, including a link or code the run
  sends when it has no outcome for the Actor without it: requesting a password
  reset and choosing the new password are one, and resending the link is a
  Scenario of it. A later act on something a run produced — confirming the
  email of an existing account — is its own. Settings in one section of the
  Product's navigation are one Capability however the screen saves them (each
  field on its own or one Save button), and different sections are separate
  Capabilities: all notification settings are one. Entries of a list, such as
  permission entries or members, are things of their own, each added, changed
  and removed by its own Capability, even inside a settings section or through
  an API call that replaces the whole list. The same verb reached from another
  context is the same Capability, available there too: changing a password the
  Product requires at sign-in is Change password, joined to sign-in by a
  Journey. A continuation several Capabilities share (a second factor after any
  sign-in method) is its own Capability: the ones it continues end their
  Scenarios at the hand-off, stating it in their Outcome, and a Journey joins
  them. Splitting neither creates nor removes a Domain.

  **Opposite verbs are separate Capabilities** — publish and unpublish, enable
  and disable, follow and unfollow, pause and resume, accept and dismiss, share
  and stop sharing, open and close — even where one button toggles: each
  control shows its own verb. Returning to an earlier State with the *same*
  verb is a Scenario (republishing is `publish-collection`). One control that
  sets one fact to one of several values — visibility private, unlisted or
  public — is one Capability, `change-<thing>-<fact>`. Changing one's own
  earlier submission through the control that made it (a vote, an RSVP) is a
  Scenario of the submitting Capability. Putting an entry on a thing's list
  and taking it off (a tag, an assignee, a member) are two Capabilities even
  inside its edit form when the entry is an Entity or a person; a value inside
  one fact (a question's options) changes with that fact; an on/off switch among a settings section's settings
  stays part of that section's one Capability. An umbrella verb — manage, organize,
  handle — hides Capabilities; name each verb its controls show.
- Capability Scenario: taxonomy `kind`, named `routes`, and ordered typed
  `steps`. Its parent Capability is implicit on every Step.
- Domain: H1, lead description, and `## Boundary`; optional `colorSlot`. A
  Domain is a region of subject matter, classifying members of the Interface →
  Experience → Screen and behavior hierarchies; Journeys may cross Domains.
  Only Capabilities and Entities author `domain:`; every other Domain relation
  is derived. Its `## Boundary` must state something the Domain does **not**
  own, and a Domain naming fewer than two Capabilities is a warning. Create
  Domains from the Product's own sections — the areas its navigation, settings
  and administration group things under — one per section holding two or more
  Capabilities (the finest navigation level that still holds two or more; a
  parent menu is a section only when none of its direct children is), named in
  the Product's words. A Capability no section reaches (signing in, or one only
  an emailed link or a schedule starts) joins the section whose Capabilities
  change the same Entities, or has no Domain when more than one section or none
  does; one alone on a page beside a sibling section has none either. A
  Capability several sections reach (a document opened from Home, Recent and its
  collection) has none, and nothing that is not a section becomes a Domain;
  never write a Domain of one Capability. For a planned Product, use the
  planned sections. Domains already in the model are the author's: add new
  Capabilities to them and never re-cut, merge or rename them.
- Naming: ids and H1s use the words the Product shows the people who use it —
  its screens, menus and messages — never API values, code identifiers or a
  request's wording (Editor is `editor` even where the API sends `member`; a
  planned Product uses its plan's words). Behavioral ids are verb-noun
  (`browse-catalog`, never `catalog-browsing`); cross-cutting ids are the bare
  noun. A Capability's verb is the one its control shows (Archive, Share,
  Publish); a form that only says Save or Done is `edit-<thing>` for a thing's
  own facts and `change-<section>-settings` for a page in the Product's
  settings, the section named as its menu shows it, never `update`. When an
  Entity, Domain or Interface id ends with a behavioral id's noun half, use the
  declared name — `install-agent-skills`, not `install-skills`, when
  `agent-skills` is an Interface. When two declared names share the noun half,
  it names their category (`send-message` beside channel and direct messages);
  Experience and Screen names never supply it: they are places, not the things
  a behavior acts on. Entity, Domain and Business Rule ids never open with a
  verb; they name what a thing is or what must remain true. A compound noun
  whose first word can be a verb (`pull-request`, `sign-in`) is a noun.
- Entity: H1, lead description, and at least one of `## Information kept`,
  `## States`, and `acts`. `## Information kept` is a bullet list of **named**
  single-line facts, each `- **Name** — prose` with an em dash as the only
  separator, names unique within the Entity; a Business Rule's `facts`, a
  Screen's `entities` entry and a Step's `entities` entry cite a fact by that
  exact name. `## States` is H3 names with prose; the first listed state is the
  one a thing starts in. **The Entity declares its states and nothing about the
  moves between them**: the lifecycle is composed from Scenario Steps, there is
  no `transitions` key, and a file still carrying one is refused.
  `## Relations` and `## Transitions` are invalid sections. Optional `domain`.
  Optional `relations`, each `{ entity, verb, cardinality }` where cardinality
  states both ends source to target — `one-to-one`, `one-to-many`,
  `many-to-many`; `many-to-one` is refused and declared from the other Entity
  instead. Declared on one side only — the inverse is derived, and two Entities
  relating back at each other is a warning. A relation may target an Entity
  that acts: ownership is a relation, `owns`, declared on the owner. **An
  Entity that acts** carries `acts: external|internal`, relative to the Product
  boundary, and with it `kind: person|system`, required exactly when `acts` is
  set. It acts when it initiates, with a goal or privilege of its own, under an
  inbound contract the Product must keep stable — a Reader, a store admin, a
  payment gateway that posts webhooks, the AI agent harness that loads a skill
  (`ai-agent`, `kind: system`, `acts: external`, never named after one use of
  it; a fixed-command CI runner does not act by analogy). A system the Product
  calls out to does not act; a privilege that exists only in code is
  authorization, not product meaning, and is never modelled.

  **AI enters in exactly one of two ways, decided by who initiates.** A model
  the Product calls — drafting, summarizing or classifying a person asks for,
  or the Product runs on its schedule — is an outbound dependency: a Product
  Step, the Capability naming the language model and what triggers the call,
  no Entity that acts, no Interface, and an `edge` Capability Scenario for what
  the person sees while the model is unavailable. A built-in "assistant" is
  this case and never an Actor. The person's own agent harness is `ai-agent`
  behind an `agent` Interface, and every grant naming it reaches the person it
  acts for through `related` (a person `connects` an AI agent); a bare
  `actors: [ai-agent]` grants every agent. What the AI produces to change
  something a person owns, and the Product keeps, is a draft (a summary that
  changes nothing is not): its own Entity when it keeps facts the target never has (a
  reason, a source passage), otherwise a `Proposed` State of the target. A
  draft Entity's States are `Proposed`, then `Accepted` or `Dismissed`, and
  `Outdated` where the Product closes it because its target changed.
  Accepting and dismissing a kept draft are each a Capability —
  `accept-<draft>`/`dismiss-<draft>` for a draft Entity,
  `accept-proposed-<thing>`/`dismiss-proposed-<thing>` for a State, where
  dismissing removes the proposed thing; one Business Rule says only
  the person decides it, another that it changes nothing until accepted. A
  draft that only fills an editor the person has not saved is not kept, and
  saving is the acceptance.

  An Entity is a thing an Actor points at and the Product tells apart —
  identity, not storage: a shopper says "this order", never "this order line",
  so the lines are information kept inside Order, and a word for all of them
  together — "library" for every saved item — is not an Entity either:
  containers and parts are not Entities. Never a data model: no types,
  no keys, no foreign keys. Also never a representation of another Entity (a
  serialization or export is that thing in another shape: if it can be
  regenerated, it is a projection), a receipt the Product keeps to work safely
  (ask who the record is for), or the Product's
  own surfaces, shipped content and closed vocabularies — where there are no
  instances, only members of a fixed list, that is a vocabulary. For a family
  of candidates sharing a word, write `## Information kept` before deciding how
  many Entities there are: one if a single list is true of all of them, several
  the moment it needs "depending on the kind" or carries a fact that holds for
  some and not others. The test reads facts, not effects: a fact whose effect
  differs by kind, such as a permission level that also covers a folder's
  contents, is a Rule and never splits the Entity. Being stored, parsed and
  rendered alike is not the test. Where one list is a subset of another the
  intersection proves nothing: ask whether the smaller one has an address of
  its own — a file, a route, a scope a command accepts, an id another resource
  cites; one with any of them is its own Entity, however firmly the larger
  thing contains it. Containment is storage,
  and the closed-vocabulary exclusion reads against the thing you would name,
  not the classification above it. When close, split. A log of what happened
  that the Product shows an Actor (an activity or audit log an administrator
  reads) is one Entity — who acted, what changed, when — presented by its
  Screen, and one Business Rule without `permits` targeting the recorded
  operations and the log Entity (a bare `{ type: entity, id }`, no `effect`)
  says what it records ("Every change to an
  Order is recorded in its activity"); no Step lists the entry its operation
  causes, while a Step reading the log lists the entries it reads. A log only
  the team running the Product reads is a receipt and is not modelled. Optional
  `singleton: true` marks the Product's own settings, kept exactly once; never
  on an Entity that acts, and never a relation's `many` end (the target of a
  `one-to-many`, either end of a `many-to-many`). An Entity must be
  changed by a Step, presented by a Screen, named as an actor somewhere, or
  read by a Business Rule as a condition's `entity` or a `configuredBy`; a
  Step's read never counts, and neither does a relation from another Entity.
- Screen: an optional stable view an Actor reaches — a place, never a component,
  layout, route mechanically discovered from source, or visual variant. Optional
  `entities`, Interface-keyed `entryPoints`, H1, lead and optional `## Intent`;
  no authored `capabilities` or `availability`. Capabilities derive from Steps
  placed exactly here (the owner for a Capability Scenario, the Step's
  `capability` for a Journey Scenario); at least one is required, and each
  must be available in the Screen's Interface or Experience (every Experience
  for a shared Screen). Each Entity entry is a bare id only for an Entity
  without named facts, or `{ entity, shows?, collects? }` with at least one
  non-empty list of unique exact fact names. `shows` is disclosure; `collects`
  is Actor input, not read permission. Both may name a prefilled editable value.
  A Screen is one working context — one selected subject or one process in
  progress — and Screens never nest. Tabs of one subject, its panels, and the
  stages of one wizard are that one Screen, its `entities` their union: tabs or
  one long page, a wizard or one form, is design, and Step order carries the
  sequence. A completion message, a generated credential reveal or a read-only
  result is an Outcome on the process Screen, not a Screen. A generic settings
  or category selector is not a subject; each settings page with a subject of
  its own is a Screen. Opening a destination from a view never makes that view its owner: a
  destination opened from several views sits once at their common Interface or
  Experience. URLs, co-visibility, modal versus page, and different drawings of
  the same information decide nothing; treat ambiguous ownership as a question
  for the author. An empty, unauthorized or blocked view is a `condition` Step,
  Edge case or Rule outcome in the Scenario that meets it, and its capture
  attaches to that Scenario. Confirmation stays behavior on its host; preserving
  the underlying view is an Outcome. `## Information presented`,
  `## Available actions`, `## View states` and `## Capability boundary` are
  invalid; other H2s are supporting content.
- Business Rule: a durable constraint, derivation, or permission — H1 and lead
  assertion, optional `## Intent` and `## Rationale`, and a non-empty
  `appliesTo` list of typed `capability`, `capability-scenario`, `journey`,
  `journey-scenario`, direct `context`, or `entity` targets. The H1 states the
  assertion about what the targets select: a permission names the operation
  and who may perform it ("Only the owner reads an unpublished collection"), an
  invariant what always holds. A consequence, a feature, or the mechanism
  behind the Rule belongs in the lead or `## Rationale`, never the title; from
  the title and `appliesTo` alone, the grants' who is no surprise. Each
  operation Actors perform on an Entity — `creates`, `changes`, `removes`, and
  `reads` where a grant restricts it — has its own permission Rule naming that
  `effect`, even where two share a grant.
  An Entity target is `{ type: entity, id, effect?, from?, to?, facts?, contexts? }`:
  **a target selects; a grant conditions.** `effect`, `from` and `to` select
  Steps by the keys their `entities` entry carries (`from` with
  `changes|removes`, `to` with `creates|changes`, neither with `reads`);
  `facts` names the facts it governs and selects only Steps citing one — with
  `creates|changes|removes`, a field-level edit of those facts; `contexts`
  resolves to existing places. Without `permits`, each place must present the
  Entity and a governed fact when fact-scoped, or contain a Screen that does. A
  fact-scoped read target selects Screens that `shows` a governed fact and
  Steps that read it; collected inputs are not disclosure. A Rule may govern a
  fact nobody currently reads, and a prohibition — including a permission Rule
  — needs resolvable references, never a matching disclosure, operation or
  example of its violation. A target `from`, or a grant `state` condition, that
  every selected Step already satisfies is a warning — the minimal selector is
  canonical. Optional `permits`: omitted means no authorization claim, `[]`
  means forbidden to everyone, a list means permitted through any one grant.
  Grants are OR, keys within a grant are AND, Rules selecting the same
  operation are AND, and an operation no permission Rule selects is open, so
  grants meant as alternatives sit in one Rule: two permission Rules with
  identical selectors are a warning ("selects exactly what … selects"). A Rule
  with `permits` targets Entities only. Every grant names a who — one of
  `actors` (Entities that act), `related` (a path of `{ verb, entity }`
  segments from the Rule's one Entity target, walking relations and their
  inverses, ending on an Entity that acts; never `[]`, which is `self: true`;
  a Rule using it, a defaulted `fact`
  or a `state` condition has exactly one Entity target, so split a Rule that
  needs more), `self: true` (the instance itself), `unattended: true` (the
  Product's own schedule), `configuredBy` (an Entity holding customer
  configuration) — plus optional `when`, a list of AND-ed conditions, each
  `{ fact, <operator>: value }` with one of
  `over|under|at-least|at-most|is|is-not|present|absent` (`present` and
  `absent` take only `true`), optionally `entity`
  to read another Entity's fact (always one instance of it, resolved as
  described with Actors above), or `{ state: X }` for the instance's current
  state (valid on every target but `creates`, needed because reads and
  information changes carry no state to select by). A value is a scalar or
  the fact holding the threshold, `{ entity: <entity-id>, fact: <fact name> }`
  (a fact that Entity keeps, read as one instance just as a condition's
  `entity` is: `over: { entity: workspace, fact: Approval threshold }`).
  Permission claims appear only here, never in
  Scenario prose. No structured `when` exists on Capability targets. The
  experiment engine is ordinary Product Entities and behavior only when running
  experiments is the Product's purpose. An outbound message (an email, push or
  SMS) is a `product` Step that `reads` the facts it carries, its other wording
  copy; messages are an Entity only when the Product schedules, retries or
  revokes them. A Rule on exactly one behavioral target with no `contexts` is a
  warning; Entity and Context targets are always valid.
  Rationale explains the current condition or consequence that makes the
  constraint necessary, never rejected designs.
- Journey: at least one unique `actors` entry, H1, no lead prose, `## Goal`, and
  `## Success criterion`. A Journey is a stable goal, not a route or Capability
  wrapper; a wizard is a Journey only when its Scenario crosses Capabilities.
  Write every Journey the test finds: one exists wherever the Product itself
  carries an Actor from one Capability into another toward one outcome — a
  redirect such as into the editor of what was just created, a required next
  Step, an emailed link to follow — and never where
  the Actor merely chooses to do something else next. Returning the Actor to
  where they were already going after signing in is not a hand-off, and neither
  is a continuation the Product runs without the Actor, such as merging
  automatically once checks pass. Neither is a hand-off to a different Actor,
  such as an invitation another person follows: the Actor carried must be the
  same one. The test is structural, so "omit rather than assert" does not apply
  to it. The carrying Step ends the run that carries, so it names that
  Capability (`create-board` for the redirect into the new board), and the
  next Capability is the first one the Actor then uses where they were carried.
  A result or pending decision shown where the Actor already is carries nobody:
  drafts appearing in the editor the Actor is working in are the drafting
  Capability's outcome. Other Actors' Steps may sit between, but only Steps the
  Journey Actor performs or is attributed count toward its two Capabilities.
  An emailed link carries only when the Journey Actor's own run sent it, and
  returning from a thing created inline to the act in progress carries
  nobody.
  To find every Journey, read each Capability Scenario's last Product Step:
  wherever it lands the Actor at a place offering another Capability they then
  use, there is a Journey. Every Journey needs achieved Journey Scenario coverage for every
  Journey Actor. It has no `entryPoints`; resolve presentation routes from the
  first Actor-owned placed Step's Context place and its Interface or Experience.
- Journey Scenario: taxonomy `kind`, `result: achieved|not-achieved`, named
  `routes`, and ordered non-empty typed `steps`. A Step may name a Capability,
  and must when its `entities` carries a `creates`, `changes` or `removes`
  effect. Every Journey Scenario, `not-achieved` included, has a Step naming a
  Capability; an achieved one traverses at least two distinct Capabilities.
- `coverage.md`: frontmatter with exactly `scope`, `method`, `covered`,
  `exclusions`, `unmapped` and `limitations`, and a body of only `# Coverage`.
  There is no status. **Coverage records which of the repository's code the
  model accounts for, and nothing else**; what the Product is or is not belongs
  in `product.md`.

```markdown
---
scope: The storefront and order services.
method: Static inspection of source and supporting documentation.
covered:
  - description: Checkout and order tracking code.
    paths: [src/checkout/, src/orders/]
exclusions:
  - description: The design system and email templates.
    paths: [src/ui/, emails/]
unmapped: []
limitations: []
---

# Coverage
```

`scope` is the breadth of code the model accounts for, in one line; `method` is
one short line on how the code was inspected, or `""`. `covered` is code whose
product behavior the model describes, `exclusions` code deliberately outside
the model because it is not product behavior (never turn skipped work into
one), `unmapped` code holding product behavior the model does not describe yet,
and `limitations` code whose behavior could not be established (not missing
behavior, and not "code was not executed"). Every entry names its code with a
non-empty `paths`, at the highest level that still guides an agent — a folder,
or a single file only when it is the area — and its description names the code
area, never a product feature. A model tied to no code yet — a Blueprint, or a
model decided before its code exists — has empty coverage: `scope: ""`,
`method: ""` and four empty lists; a scope or method with no entry is an
error, and the first entry written must bring its `scope`. Coverage is written
by mapping code, never by deciding intended behavior. Each entry
is `{ description, paths }`: a one-line description, unique across all four
lists, of one code area with all its paths. Paths are repository-relative and
directories end in `/`; no traversal, `*` or `?` wildcards, URLs, backslashes or
fragment/line suffixes, while brackets, as in `pages/[id].vue`, are ordinary.
An empty `unmapped` list never means complete, and known gaps never relax
structural checks. A Blueprint carries no coverage: its code, if any, is
another repository's.

Both Scenario types have no lead prose, author `routes` and `steps` in
frontmatter, require `## Trigger` and `## Outcome`, and forbid Markdown
`## Steps`. Optional `## Edge cases` is a non-empty single-line bullet list.
Journey-only Goal and Success criterion sections are invalid on Scenarios, and
Scenario-only sections on Journeys. Optional `## Decision points` uses an H3
title, a question, and at least two `condition → outcome` branches that
converge on the Scenario's one result; a branch that changes the Capability
sequence or terminal result is a separate Scenario. `kind` describes the nature
of the variation and `result` the terminal Journey goal outcome; they are
orthogonal.

Each structured Step needs single-line `text`, `kind: actor|product|condition`,
and `entities`. An `actor` Step requires `actor`, an Entity that acts, who
performs it; a `product` or `condition` Step may carry `actor`, the Actor it is
attributable to — the Product did it for them. Every actor named joins the
Scenario's Actor set. A Step at a place in a public context names the Actor who
is not signed in (an invitee or role holder is that Actor until signed in), and
a Step that ends a session (signing out, deleting one's account) stays where
the session was, never on the public page the person is sent to. A Scenario
with no actor Step needs an unattended trigger: a first `condition` Step with
`unattended: true`, and then no Step carries `actor`; behavior the Product runs
on its own — a schedule it owns, an expiry, a retry — is such a Scenario,
available where an Actor observes the outcome.

**`entities` is required on every Step** and `[]` when it touches nothing.
The one thing a Step never lists is the log entry a recording Business Rule
makes for it; reading the log is an ordinary read.
Each entry is `{ entity, as?, effect, from?, to?, facts?, with? }`: `effect` is
`creates|changes|removes|reads`, defaulting to `changes`; `creates` takes `to`,
`removes` takes `from`, `changes` takes both or neither, `reads` neither. Every
state resolves; creating or removing an Entity with `## States` requires its
`to` or `from`, and an Entity without States takes neither. `facts` is required
on reads, changes and creation: the exhaustive unique list of named Product
facts affected, including defaults on creation, never inferred from form fields
or padded with incidental implementation data; `[]` names no facts, never unspecified. Removal has no
`facts`. A creation or removal that happens because another of the same Step
and effect does — a card's comments going with the card, a new board's first
membership (its creator's, as Admin) going with the board, a new page's first
revision — says `with: <alias or entity id of that entry>`, naming the Entity
that holds it: the named Entity must declare a `one-to-many` or `one-to-one`
relation to the dependent's (itself, for two of the same Entity), so `comment`
goes `with: card`, never the reverse. Put a thing created only because another
is in that creation's Step. The creation or removal it goes with is what needs
permission, so no grant is written for the dependent; two creations or two
removals where one holds the other with no `with` are a warning, a `with` chain
that loops back to where it started is an error, and each entry's
`as`-or-`entity` reference is unique in its Step. An `actor` Step reading facts
on a Screen must find each in that Screen's `shows`, and a presence-only read
(`facts: []`) needs the Entity in the Screen's `entities` unless that Entity
acts; Product and condition Steps may consult undisplayed facts. A Step lists
every thing it moves, one entry per `(entity, as)` pair; `as` is a scenario-local alias for two
instances of one Entity, and once used, used everywhere in the Scenario. Where
an earlier Step left a pair in a state, a later Step's `from` for it must
match. `reads` is a bare mention: no state, never a change, never enough to
keep an Entity from being an orphan. Author the effects on the Step that
performs them. After drafting, re-read every Step's `text` against the Entity
list: a Step whose text names an Entity title it does not declare is an error,
exempting its own `actor` and the phrase "The Product". A Step performing an
operation a Business Rule governs must have an actor with a possible grant,
and a Step performing one a Rule closes with `permits: []` is an error; an
unattended Step needs an `unattended` grant. A Screen presenting what a read
permission Rule governs (collected inputs aside) needs an Actor of its
Interface or Experience with a possible grant, and presenting what a Rule
closes with `permits: []` is an error.

A Capability Scenario in full. One route still declares `routes`; Steps carry
`text`, `kind` and `entities`, an `actor` Step names its Actor, and each Step
maps every declared route id to one Context place:

```markdown
---
kind: primary
routes:
  web: Web
steps:
  - text: The Reader enters a name
    kind: actor
    actor: reader
    entities: []
    contexts:
      web:
        place: reader-web::personal-library::collection-workspace
  - text: The Product creates a private collection owned by that Reader
    kind: product
    actor: reader
    entities:
      - { entity: collection, effect: creates, to: Private, facts: [Name, Item order] }
    contexts:
      web:
        place: reader-web::personal-library::collection-workspace
  - text: The empty collection is ready to edit
    kind: condition
    entities:
      - { entity: collection, effect: reads, facts: [] }
    contexts:
      web:
        place: reader-web::personal-library::collection-workspace
---

# Create an owned collection

## Trigger

The Reader chooses to organize saved items in a new collection.

## Outcome

The Reader has a new private owned collection with the chosen name.
```

`kind` names an id declared in `taxonomies.yaml`. The containing
`capabilities/<capability-id>/scenarios/` directory is the only parent
authority, so no `capability:` field is written. A Journey Scenario has the same
shape plus a required `result`.

An Entity that acts, in full. `kind` is required exactly because `acts` is
set; the relation is declared here, on the owner, and its inverse is derived on
Order:

```markdown
---
kind: person
acts: external
relations:
  - entity: order
    verb: owns
    cardinality: one-to-many
---

# Shopper

A person who browses the catalog and buys products.

## Information kept

- **Delivery address** — where their orders are sent
```

A Business Rule with an Entity target and grants, in full. The target selects
every Step carrying `{ entity: order, effect: changes, to: Refunded, facts: [] }`; the
grants say who may perform it, and `when` conditions the grant it sits in:

```markdown
---
appliesTo:
  - type: entity
    id: order
    effect: changes
    to: Refunded
permits:
  - related: [{ verb: owns, entity: shopper }]
    when:
      - { fact: Total charged, under: 100 }
  - actors: [store-admin]
---

# Refunds above the threshold need an operator

A shopper refunds their own order while its total is under 100; a store admin
refunds any order.
```

`related` walks the Shopper's `owns` relation back from Order to an Entity that
acts; `self: true` would require Order itself to act, so here it is an error. A
`state` condition cannot be combined with `entity` and is invalid on a
`creates` target. The lifecycle the Rule governs is composed from Steps: a
Step's `{ entity: order, effect: changes, from: Confirmed, to: Refunded, facts: [] }`
puts Refunded on the machine. `lint` composes every Scenario and warns on an
**unreached state** — a state other than the first that no Step leaves anything
in — and an **unproduced origin** — a Step leaving `from: Confirmed` when
nothing produces Confirmed and it is not the first state.

Context is the single model concept for where behavior applies. In schema 11 it
is a strict object containing one `place` field. A Capability's availability
Contexts name an undivided Interface or an Experience:

```yaml
availability:
  - { place: reader-web::personal-library }
  - { place: reader-mobile::personal-library }
  - { place: operator-cli }
```

An Experience belongs to exactly one Interface, so its id already names it. A
Context place either names a declared Interface, Experience, or Screen or it
does not. Availability is intended Product meaning, not
implementation status.

**Two conditions decide whether an Interface is divided.** It holds
Experiences when either holds, and none when neither does:

- it serves more than one `access` value, read from who reaches its places:
  places reached without signing in, including wherever people sign in, are
  one `public` context; places reached once signed in one `authenticated`
  context; the administration area — places only the roles that administer
  the Product, its settings or members, may enter — one `restricted` context;
- its Actors split into groups no Capability available there bridges — a
  Capability bridges the Actors its Scenario Steps name, and roles that each
  relate one-to-one to the same Account are one audience.

An Interface declares no access mode, so `lint` sees the first condition only
once Experiences exist: reading access from who reaches each place is the
author's. It checks the second on every Interface. Two Experiences of one
Interface with the same `access` share no Actor unless they are alternatives of
one Variation. A page closed only to some non-administrative roles stays
`authenticated`, and an admin-only page inside administration is a Screen
there: grants say who may act. A counterpart (an Experience whose name also
exists under another Interface) and a Variation
alternative keep their Experience even where the conditions alone would
flatten it. An Interface that must divide and does not, and one holding
Experiences it must not, are `lint` errors.

An Interface holds `screens/`, `experiences/`, or both. A Screen beside
`experiences/` is shared: it is inside every Experience of that Interface, so
every Capability it exposes must be available in each of them, and a Step on it
(`interface-id::screen-id`) counts as coverage for each. A view whose
Capabilities differ by Experience is two Screens, one under each.

Business Rule Contexts use the same object shape:

```yaml
context: { place: reader-web::personal-library }
```

Use a bare Interface id for an undivided Interface and
`interface-id::experience-id` for an Experience; there is no separate
`experience` field. A resource Rule target may omit `contexts` to cover all
target contexts or provide a non-empty list to narrow it. Targets are additive.
A Capability plus one of its Capability Scenarios, or a Journey plus one of its
Journey Scenarios, is redundant and invalid. Domains are derived Rule
backlinks, not authored targets.

Each Scenario route maps a stable kebab-case id to a human name. A placed Step
maps every route id to its most-specific Context; a Step without `contexts` is
shared by all routes and has no Context. Name the Screen when the Step occurs
on one, otherwise the leaf Experience or undivided Interface even if other
behavior there has Screens. Every route is placed at least once and
no two routes repeat one place sequence. A place change between
consecutive placed Steps is a Context place transition.

Scenario Actors, Entities, availability places, Screen participation,
transitions and backlinks derive from Steps; Screens never author Scenario ids.
Every Actor must be supported by at least one selected Context place, every
Step's `actor` by each Context place that Step names, every
derived availability place must support a Scenario Actor, and a
Capability-bearing Context place must be inside that Capability's
availability. A Capability need not have a Screen. Every Journey route begins
its Actor-owned placed Steps with a Journey Actor.

Every semantic resource may contain optional `references`. Each strict item
needs `kind: code|prd|spec|proposal|doc|adr|visual|research`,
`role: intent|implementation|context`, `target`, and optional `title`. Code
targets use `path[#symbol][:start[-end]]` and their path must be tracked; other
targets use HTTP(S) or a repository-relative path. Duplicate targets on one
resource are invalid, and References carry no `state` key. References are
attachments, never proof or lifecycle state. Coverage, config, and taxonomies
do not accept them.

## Structural findings

All structural checks apply regardless of Coverage: unresolved references,
duplicate entries, and an authored `screens` key on a container are errors.

`.gitignore` contains `build/` and `cache/`.

## Canonical `product-model/README.md`

Write this orientation for every new Product Model:

```markdown
# Product Model

This directory is the **Product Model** of this repository, kept with
pdd-kit for Product-Driven Development (PDD): who the product serves, where
they reach it, what it keeps, what it can do, and which rules must stay true. It
is plain Markdown tracked in Git, and it is the source of truth for the intended
product definition.

## If you are an agent working in this repository

- **To implement it**, all of it or a part, work the way you and the user
  normally do. The model is the acceptance contract: see *What the code must
  honor* below. When you finish, recommend checking the code against the model
  with the `pdd-verify` skill; do not run the check unless asked.
- If the repository's agent instructions or the request say "Implement with
  pdd-verify: plan the work from `product-model/` in phases and check each
  part before the next.", or the installed `pdd-verify` skill says this
  install orchestrates implementation, implement with `pdd-verify` instead,
  unless the request says to implement without it.
- **To check the code against it**, use `pdd-verify`. **To change
  what the product should do**, use `pdd-ideate`. To model established
  code the model does not cover, use `pdd-map`.
- If those skills are not installed, run `npx pdd-toolkit install`.
- Do not edit this directory while implementing. If the model is ambiguous or
  seems wrong, stop and ask; never settle a product question in code.
- Never edit `cache/`. Run `npx pdd-toolkit lint` for structural checks.

## What the code must honor

- Capability Scenarios are acceptance tests for one Capability; Journey
  Scenarios are end-to-end tests across Capabilities.
- Business Rules must stay true. A Rule's `permits` names exactly who may
  perform an operation; everyone else is refused.
- A Screen's `shows` and `collects` are the facts it presents and the input
  it takes, not its layout or design.
- `availability` names the Interfaces and Experiences where behavior must
  work.
- An Entity's facts and states are what the product keeps and distinguishes,
  not a storage schema.
- A Variation's `selectedWhen` says what chooses each supported alternative.

## What it does not prescribe

- The stack and architecture: choose them yourself.
- Domains group the model by subject; they are not modules or services.
- References are navigation and context. They never prove the code matches and
  never prescribe a design.
- `coverage.md` says which of the repository's code the model accounts for,
  never whether that code matches it; it stays empty until code is mapped.

## Reading order

Start with `product.md` or `product/product.md`, then Entities, Interfaces
with their Experiences and Screens, Domains, Capabilities and their Scenarios,
Business Rules, Journeys and their Scenarios, and Variations. A resource is
`<id>.md`, or `<id>/<type>.md` when it owns child resources or assets.

Documentation: https://pdd-kit.dev
```

## Variations

A Variation is a named set of two or more currently supported alternatives of
one resource type: Interface, Experience, Screen, Entity, Capability, Capability
Scenario, Journey, Journey Scenario or Business Rule. Product, Domain and
Variation itself cannot vary. The set is its own resource,
`variations/<id>.md`; alternatives stay ordinary, independently complete
resources and carry no Variation keys.

`variations/refund-review.md`:

```markdown
---
kind: configuration          # experiment | configuration | version
of: business-rule            # the one member type
settings:
  - { entity: store-settings, fact: Refund review mode }
takesEffect: When a refund is requested. A settings change applies to subsequent requests.
stability: A refund already under review keeps the policy captured when it was requested.
alternatives:
  - id: refund-review-standard
    selectedWhen: Refund review mode is Standard. Missing mode uses Standard.
  - id: refund-review-strict
    selectedWhen: Refund review mode is Strict.
---

# Refund review

Stores choose how strictly refunds are reviewed; both policies are supported.
```

The H1 names the choice; the lead says why the alternatives coexist. `## Intent`
is optional.

**Membership lives only on the set.** `alternatives` lists at least two distinct
resources of the type `of` names, by that type's ordinary ids (a Screen's full
`interface::experience::screen`). A resource belongs to at most one Variation.
The list is a set; its order means nothing.

**Vary the smallest resource that fully contains the difference.** Two Scenarios
of one owner when what an Actor does differs; Capabilities when the contract
differs — who may do it, its verb — or where it is offered, never for a
different Actor alone; Screens or Experiences when the place differs; Entities
when the facts or States kept differ; Business Rules only when a setting
switches between whole policies, below. The alternatives of a Scenario Variation
share their Capability or Journey; alternatives spread over several owners are a
`lint` error. A Step has no id and is never an alternative. Steps, Screens and
Rules name concrete resources, so a varying Entity carries into what touches it:
a Step that creates one alternative sits in a Scenario selected the same way.
When only the path a thing takes differs, the Scenarios vary and the Entity
keeps every State.

**What selects decides whether it is a Variation.** A Variation chooses by a
fact that exists to choose — a setting, an experiment assignment, a version
discriminator — or by the deployment, fixed before the behavior starts:

- A fact describing the thing acted on — a page's own editor format — is
  state, even when someone set it earlier; a `condition` Step or decision point
  reads it.
- A choice that changes what an Actor does — a Step skipped, added, or at
  another place — selects Scenario alternatives, even when several settings
  combine into that one choice (a person's preference falling back to the
  workspace's): sign-in that starts at the only provider drops the Actor's
  choice of provider. A choice the Actor makes on a page outside the
  Product, such as a connector at an identity provider, is still an Actor Step.
- A choice that changes only the Product's own Steps (group sync replacing or
  adding Roles), and any choice made during a run, is a decision point in one
  Scenario — unless a Step must name a different alternative of another
  Variation, which then varies with it (issuing a VAT invoice or a sales tax
  receipt).
- A choice that changes only the outcome makes separate Scenarios, each with a
  `condition` Step reading it: registration that leaves the account
  unconfirmed, an unknown social account registered or refused. A branch ending
  in a refusal counts. The unconfirmed account sign-in later meets is state,
  and confirming through the emailed link is a later act of its own.
- When two or more independent settings or assignments would each vary or split
  one Scenario (a captcha and a provider password on one registration), each is
  a decision point instead, so no Scenario needs a set per combination. State
  the Scenario meets, such as whether an account exists, is not a setting.

**A resource that exists only under some alternatives, or only while a
setting, plan or licence enables it** — registration while the sign-in method
is password, social sign-in while a provider is configured, a guest role a
paid plan enables — stays an ordinary resource, modeled even where the running
or planned edition hides it. Its lead names what it exists under, and `verify` checks it.
No field or Variation carries the dependency. What it does to Entities is
closed by the switch through grants as well: every grant that admits only the
gated behavior's Steps carries the switch as a `when` condition — each grant
of the refund Rule (refunds are the only behavior moving an Order to
Refunded), and the `unattended` grant through which the Product sends what a
Pro workspace scheduled, even inside an ungated Capability. A gated operation
no Rule governs yet gets one (picking a send date changes a fact nothing else
changes). A read-only feature, or one whose Steps pass only grants ungated
behavior passes too (archiving many Cards at once beside archiving one), has
its lead alone.

`kind`, `of`, `takesEffect`, `stability` and `alternatives` are required, and
so are each alternative's `id` and `selectedWhen`.
**Each selection field has exactly one level.** All text is a non-empty Markdown
fragment without H1/H2.

| Field | Level | Meaning |
| --- | --- | --- |
| `selectedWhen` | alternative | Eligibility and the choice selecting this alternative, including missing/unsupported choices and defaults |
| `takesEffect` | set | When selection is made or re-evaluated, including changes during use |
| `stability` | set | How long the choice remains fixed and what happens to existing clients, sessions or records when it changes |

`kind` decides the remaining fields; a field outside the subtype is a `lint`
error. A fact reference is exactly `{ entity: <id>, fact: <name> }` and must
resolve to an Entity and one of its Information kept facts.

| `kind` | Set fields | Alternative fields |
| --- | --- | --- |
| `experiment` | Required `assignmentUnit` (`{ entity: <id> }`, or `{ description: <text> }` for an unmodeled unit such as a session) and `assignmentMethod`. Optional `assignmentFact` and `allocation`. | — |
| `configuration` | Optional non-empty `settings`: distinct facts that choose between the alternatives. A fact that only tunes one alternative's behavior is not a setting. | — |
| `version` | Optional `discriminator`: the kept fact identifying the version. | Required `label`, unique in the set ignoring case. |

The subtype states why alternatives coexist: Experiment evaluates outcomes;
Configuration selects through a setting or operating context; Version keeps
contracts or forms live together. A version selected by a setting stays
Version; an experiment enabled by a setting stays Experiment. Header or path
selection is explained in `selectedWhen`; do not invent an Entity to hold it,
and do not create Entities solely to fill these fields.

**When a Variation is justified.** Two or more resources of one type are all
supported now and something decides which applies. Otherwise: a parameter value
(a threshold, a limit) is content of the one resource; phases are Entity
States; the same Experience on another Interface is a counterpart; a look-only
treatment is a `visual` Reference. No alternative is a default, parent or
historical version. When one alternative is left, remove the Variation and keep
still-relevant meaning in ordinary content.

A Variation is a relation, never containment: folders, Domains, Experience
ownership, Screen placement and Scenario parents stay as they are, and Rule
targets, Steps and Contexts keep naming concrete resources. A Business Rule
that is an alternative applies only under its `selectedWhen`, never
unconditionally, so `lint` does not check its grants against Steps and Screens;
`verify` does. Rules vary only when a setting switches between whole policies —
refund review that is Standard or Strict, a password rule of a minimum length
or of a length plus required kinds of character; a value one policy reads stays
content of that Rule. A setting that changes only who may perform one operation
is a grant's `when` on one Rule. A Variation grants no permission. An Entity
that is an assignment unit or holds a fact a Variation chooses by is not an
orphan.

**Evidence, not invention.** Record only selection the evidence or approved
intent establishes; never invent Entities, settings, allocations, defaults or
timing. Omit an optional field the evidence or approved intent does not
establish, say so in a
required one such as `takesEffect`, and, when the model was mapped from code,
record that code under Coverage `limitations`. `lint` validates structure and references, never whether conditions
are exhaustive.
