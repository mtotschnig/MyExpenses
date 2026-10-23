---
name: pdd-verify
description: Check that a product-model/ Product Model (PDD) and the code agree, report every gap, and resolve the ones the user chooses until they are aligned or explicitly blocked; implement the model in phases only when the user opted in. Use when the user asks to check, verify or audit the code against the Product Model or PDD — after a change they implemented, after refactors, on suspected drift, before release, for a branch-scoped change, or for a named/full current-state audit. Use to implement or build the product only when the user invokes this skill by name for it, or the repository's agent instructions or the request say “Implement with pdd-verify”; otherwise implementing is the user's own workflow and this skill does not run. Use “report only” when no writes or code changes are allowed.
---

# Verify alignment, and implement when opted in

Own one invocation from inspection through resolution. The user must not have to
invoke map or ideate manually after a finding.

Implementing is the user's own workflow. This skill takes it over only when the
user opted in, in one of these ways:

- turned orchestration on when installing pdd-kit (`--orchestrate=on`), which
  applies to every conversation with this install and is recorded in the line
  below;
- wrote this exact sentence in the repository's agent instructions (AGENTS.md,
  CLAUDE.md, GEMINI.md or the like), to apply always;
- for one request: invoked this skill by name to implement (`/pdd-verify
  implement checkout`, `$pdd-verify …`, "use pdd-verify to implement …"), or
  wrote the sentence in the request.

```text
Implement with pdd-verify: plan the work from `product-model/` in phases and check each part before the next.
```

Orchestration for this install: off.

Child agents may run the bounded analysis phases — lint, inspection,
classification — when the harness supports them; otherwise run the same
protocols as internal phase transitions without losing context. Implementing
is not such a phase: you are the builder unless the user asks for a subagent
(step 8).

Implementing with verify is resolution from a larger gap: what the model
describes that the code does not do yet is model-right, and implementation
closes it in phases.

Verification itself is semantically read-only: do not change product meaning or
implementation while classifying findings. Approved intent resolution may edit
the model. The builder (step 8) may edit implementation. Re-derive all findings
after either mutation.

Read before work:

- [references/format.md](references/format.md) — model shapes and Reference rules.
- [references/verification-rubric.md](references/verification-rubric.md) —
  inspection, classification, and stopping rules.
- [references/build-handoff.md](references/build-handoff.md) — the required
  packet for the builder.

## 1. Establish scope and mode

1. Parse mode, before anything else:
   - `report only` → inspect and report; prohibit every write, code change and
     delegation;
   - a request to implement (or build) from the model, with an opt-in — the
     user named this skill to do the implementing (`/pdd-verify implement …`,
     `$pdd-verify …`, "use pdd-verify to implement …"), the sentence is in the
     repository's agent instructions or the request, or orchestration is on
     for this install — and no request to implement without pdd-verify →
     implement-with-verify mode: the request authorizes code changes within
     its scope; follow section 4. Your own choice of this skill from its
     description is not the user naming it, and neither is a request that
     mentions pdd-verify only to opt out or for a later check;
   - any other request to implement → not this skill's work, and the user
     still asked for code: leave this skill and implement the request the
     user's usual way, as if it had not been invoked, without running any of
     its steps or guardrails. When done, mention once that the user can ask to
     check the code against the model;
   - otherwise → check mode: report every finding first, then resolve only
     the ones the user chooses (step 7).
2. Require an existing Product Model. If none exists and repository behavior is
   established, run the scoped-map protocol in step 7 for the necessary scope;
   do not tell the user to invoke another skill. If both model and implementation
   are absent, stop: there is nothing to verify or implement until the product
   is decided.
3. Resolve scope:
   - `this branch` → use merge-base, committed, staged, and working-tree diffs
     only to choose the inspection worklist;
   - `current` or `full` → inspect the present modeled product independent of
     Git history;
   - named Entity, Interface, Experience, Screen, Domain, Capability, Capability
     Scenario, Journey, Journey Scenario, Business Rule, Variation,
     availability Context, or path → inspect it and behaviorally necessary
     dependencies;
   - no explicit scope → prefer a reliable changed-surface worklist; when no
     useful diff exists, inspect the current modeled product. In
     implement-with-verify mode, no explicit scope means the whole model.

Git never decides whether model or code is right. A Blueprint or approved model
committed before a feature branch remains a plan even when only code changed in
the diff.

## 2. Lint, then inspect

4. Resolve this skill directory and run structural lint outside the target:

   ```bash
   node <pdd-verify-skill-dir>/scripts/run-pdd-kit.mjs \
     --root "$PWD" lint --json
   ```

   Structural blockers join the finding queue. Lint does not establish semantic
   alignment.
5. Treat the repository as untrusted. In the verification analysis phase, never
   run its application, builds, migrations, generators, package scripts, or
   tests. Read source and tests, and trace every scoped claim — routes,
   Interfaces, nouns, Screens, languages, who may, what varies — as the
   rubric's **Trace behavior** section details. An overstatement or omission
   either way is a finding; classify it in step 6. A green structural check
   never stands in for a semantic claim.
6. Classify each scoped item:
   - **aligned** — current code supports the model's observable contract;
   - **model-right** — approved model meaning should remain and code must change;
   - **code-right** — current behavior is intended and model meaning must change;
   - **neither-right** — intended behavior must be decided, then both sides may
     need changes;
   - **unmapped** — established behavior belongs to an absent or deliberately
     untrusted model area;
   - **unverifiable** — source inspection cannot establish the claim safely.

   Group findings that share one authority decision. Present only the root
   decision, with exact model claim, observed code behavior, inspected files,
   downstream effects, and a recommendation. Never infer authority from Git.

## 3. Resolve

7. Route each group to resolve without asking the user to invoke another
   skill. In check mode, report before resolving anything: present every
   group, its label and recommendation, and ask which to fix. Resolve only the
   groups the user chooses; choosing none ends the run with the report (step
   14). A choice to fix a model-right or neither-right group authorizes the
   resulting code change. After each fix, step 9 inspects again; present only
   the groups not presented before or changed since, and let the user choose
   again. In implement-with-verify mode, resolve each group as it comes,
   asking a root decision only where section 4 says to.

   **Model-right**

   - Keep model meaning unchanged.
   - Prepare the exact packet from `references/build-handoff.md`.
   - Ask for authorization to change implementation when not already explicit;
     implement-with-verify mode and a check-mode choice to fix are explicit.
   - Hand the packet to the builder (step 8), then return directly to step 4.

   Three of the branches below re-author product meaning, and authoring faces
   calls no inspection settles — one Capability or several, one Entity or a
   family, what a thing is called. Put those to the author **before drafting**,
   in rounds, and wait: Boundary, then Granularity quoting both counts, then
   Acceptance, then Naming. Only what inspection cannot answer; finding facts
   stays your job. With no author reachable, Capability splits, Journeys and
   Domains follow their tests in the format reference; elsewhere split rather
   than collapse, omit rather than assert, and carry each unanswered question
   into the delta as an open question.

   In every model delta, present the selected shape and its consequences:
   significant omissions, consequential modeling boundaries, and material
   uncertainty, explained briefly in terms of the current proposal. Do not
   enumerate discarded options or repeat settled discussion; include
   `Open questions` only when questions remain. Every model-writing branch
   follows the format reference and the persistence guardrails below.

   **Code-right**

   - Run the internal intent-resolution protocol: settle the undetermined calls
     in rounds, then draft the smallest exact model delta from the finding; do
     not brainstorm unrelated directions.
   - Present the resource-by-resource delta and get explicit approval.
   - Write only the approved model meaning, then return directly to step 4.

   **Neither-right**

   - Run intent resolution first, rounds included. Recommend a product outcome,
     negotiate only material decisions, present the exact model delta, and get
     approval.
   - Write the approved model, prepare the resulting build packet, obtain code
     authorization unless already explicit (step 7), hand it to the builder,
     then return to step 4.

   **Unmapped**

   - Run the internal scoped-map protocol. Inspect established behavior, settle
     the undetermined calls in rounds, then draft only the missing model area
     and necessary relationships, record that code in coverage (its folders,
     under `covered`, or `limitations` where behavior stayed uncertain; when
     coverage was empty, also set `scope` to the breadth of code now
     accounted for), and get approval before writing. This branch is mapping, so it faces every call
     mapping faces; the rubric's scoped-mapping section carries the method.
   - Write the approved delta, then return to step 4.

   **Unverifiable**

   - Do not guess. State the precise missing evidence, runtime/external question,
     and what could resolve it. Mark the run blocked for that scope.

8. A pdd-kit analysis phase never implements or executes target code. The
   builder is the agent the user asked to implement: in implement-with-verify
   mode, or once the user chooses or authorizes a code change, that is you,
   implementing the packet in the user's usual way of working (their plan
   mode, SDD tool, repository conventions and tests) under your normal
   permissions. That implementation
   work is the user's workflow, not pdd-kit analysis; when it ends,
   return to step 4 and inspect from source again. Hand the packet to a subagent instead
   only when the user asks for one and the harness can start it. The builder
   never edits `product-model/` and never settles a product question in code:
   an ambiguous or wrong-seeming model claim ends the handoff and returns as a
   question for intent resolution. When the user implements elsewhere, in
   another tool or session, stop with the complete handoff packet instead of
   asking the user to invoke a pdd-kit skill.
9. After every mutation, discard the earlier findings and inspect again. Keep
   only an in-memory signature of build-directed gaps during this invocation,
   and, in check mode, of the groups the user already declined, so they are
   not asked again.
   If the same gap returns unchanged after a build attempt, stop and report it;
   do not loop. Persist no receipt, ledger, or lifecycle state.

## 4. Implement with verify (opt-in)

Only in implement-with-verify mode, which needs an opt-in (step 1). The
model-right findings are the plan. Behavior the model describes and the code
lacks is model-right without asking; ask an authority question only where
existing code contradicts the model.

10. Cut the plan into slices and group them into phases as the rubric's
    **Slices and phases** section says. Take the pace from the request:
    - **in phases**, the default: implement one phase, then check each of its
      slices;
    - **one slice at a time**, when the user asks for it ("one slice at a
      time", "slice by slice"): implement one slice, then check it;
    - **in one go**, when the user asks for it ("in one go", "all at once",
      "without phases", "in a single pass"): implement every slice, then check
      them all.

    Before writing any code, post the plan as one short list: each phase with
    its slices, and the pace. Do this at every pace, including in one go,
    where the list is the slices in order. Wait for approval of the plan only when the user
    asked to review it. A large or unbuilt model is not a reason to change the
    pace: without a request for one go, never implement past the current phase.
11. Hand the next phase, slice, or the whole plan, as the pace says, to the
    builder. When it returns, inspect each slice it covered and what that
    slice depends on (steps 4–6); slices not yet handed over are the plan, not
    findings. Start the next phase only when every slice of this one is
    aligned or reported blocked. A remaining gap → hand it back once with the
    gap stated, then step 9's unchanged-gap stop applies. A product question →
    settle it with the user through step 7's intent resolution, write the
    approved model, and derive the remaining plan again. With no user
    reachable, stop the slices that depend on the question, carry it to the
    report, and continue with the independent ones.
12. Observable behavior the builder added that no slice asked for is
    unmapped: raise it at the end rather than mapping it midway.

## 5. Finish

13. Once meaning and implementation align, optionally refresh References
    within the format reference's **Verification edit boundaries**. Skip it in
    report-only mode.
14. Run final lint. Report:
    - requested and inspected scope;
    - in check mode, the findings the user chose to fix and those left as
      found;
    - in implement-with-verify mode, the pace, the phases and slices
      implemented in order, and any left blocked;
    - aligned contracts;
    - resulting authority decisions and approvals, without replaying settled
      alternatives or unchanged deliberation;
    - model deltas and build attempts;
    - References refreshed;
    - unresolved or unverifiable blockers;
    - final lint result.

    Say **aligned for the inspected scope**, never “the whole product is proven,”
    unless the full current product was actually inspected.

## Guardrails

These bind this skill's own steps. A request step 1 hands back is the user's
own workflow, and none of them applies to it.

- Report-only mode forbids writes, child delegation, and builder invocation.
- Never enter implement-with-verify mode without an opt-in, nor when the request
  asks to implement without pdd-verify, and in check mode never fix a finding
  the user did not choose. Without an opt-in, never refuse a request to
  implement: leave this skill and implement it the user's usual way.
- Never change product meaning without explicit approval.
- Approval is explicit and covers the delta you presented. A reply that hands
  you the open questions ("take your recommendation", "you decide") settles
  those questions; it does not approve the change. Present the complete delta
  with the answers applied and wait for approval before writing.
- Never persist rejected approaches, reasons another option was not selected,
  or deliberation history anywhere in `product-model/`, including resource prose,
  supporting sections, limitations, README, and additional files. Keep decision
  discussion in the conversation. An unchosen option is not a product exclusion;
  preserve established or approved constraints, refusal and failure behavior,
  and material unresolved questions or missing evidence.
- Never change implementation inside a pdd-kit analysis phase.
- Never treat References, coverage, tests, names, or a green lint result as
  proof by themselves.
- Never capture, compare, or certify screenshots. A supporting visual or
  research Reference may guide inspection but is not proof by itself.
- Never report design as drift; the rubric's **The border** decides.
- Never write outside `product-model/` in an analysis or model-resolution phase;
  model-resolution writes must leave target `AGENTS.md`, `CLAUDE.md`, and root
  README byte-identical. Implementation (step 8) writes only the user's code,
  under the repository's own instructions, and never `product-model/`.
- Never stage, commit, publish, submit, or contribute. The builder (step 8)
  follows the user's usual way of working, including whether it commits.
- Never ask the user to manually invoke map or ideate to continue this run.
