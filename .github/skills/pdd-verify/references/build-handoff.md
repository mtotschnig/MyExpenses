# Build handoff

Give the builder one self-contained packet: yourself, working in the user's
usual way, or the subagent the user asked for.

- **Scope:** in check mode, the findings the user chose to fix; when
  implementing with verify, the slices this handoff covers (one slice, a
  phase, or the whole plan, as the pace says), in order, and the implemented
  slices they depend on.
- **Expected behavior:** the exact approved model contract.
- **Affected model resources:** IDs of relevant Interfaces, Experiences,
  Screens, Entities, Capabilities, Capability Scenarios, Rules, Journeys,
  Journey Scenarios, Variations, and declared availability Contexts.
- **Observed gap:** current behavior and why it differs; for a slice not implemented yet,
  that the behavior does not exist yet.
- **Acceptance criteria:** observable trigger, typed steps, decisions, outcome,
  edge cases, applicable invariants, each Scenario route's most-specific
  Context places where relevant, and for a Variation each alternative's
  `selectedWhen`.
- **File leads:** inspected paths and symbols as leads, never mandatory design.
  The model names no stack; where the repository has none yet, the builder
  chooses it.
- **Constraints:** do not edit `product-model/`; preserve unrelated user work;
  follow repository instructions; never settle a product question in code:
  when the model is ambiguous or seems wrong, stop and return the question.
- **Verification:** the builder may run the target's normal tests and checks
  under its normal permissions and reports files changed, checks run, results,
  and remaining uncertainty.
- **Return:** hand control directly back to this verification invocation,
  with any product question raised.

If the user implements elsewhere, in another tool or session, return this same
packet to the user as the blocker. Do not pretend implementation completed and
do not substitute a pdd-kit skill for the builder.
