# ADR-0001: Designer-LLM ⊣ Design Delivery Governor architecture

## Status

Accepted. `cloud-itonami-isic-7410` promoted from `:blueprint` to
`:implemented` in the `kotoba-lang/industry` registry.

## Context

`cloud-itonami-isic-7410` publishes an OSS business blueprint for
specialized design activities: fashion, industrial, graphic and
interior design services for clients. Like every prior actor in this
fleet, the blueprint alone is not an implementation: this ADR records
the governed-actor architecture that promotes it to real, tested code,
following the same langgraph-clj StateGraph + independent Governor +
Phase 0→3 rollout pattern established by `cloud-itonami-isic-6511`
(life insurance) and applied across fifty-six prior siblings, most
recently `cloud-itonami-isic-7210` (research and experimental
development on natural sciences).

## Decision

### Decision 1: single-actuation shape

This blueprint's own README, business-model.md and operator-guide.md
consistently name only ONE real-world act: "releasing a final
deliverable to a client." Matching `leasing`/`underwriting`/`testlab`/
`clinic`/`veterinary`/`funeral`/`parksafety`/`salon`/`entertainment`/
`facility`/`consulting`/`advertising`/`polling`/`research`'s single-
actuation shape, `high-stakes` here is a one-member set,
`#{:actuation/release-deliverable}`.

### Decision 2: entity and op shape

The primary entity is a `project`. Four ops: `:project/intake`
(directory upsert, no capital risk), `:brief/verify` (per-jurisdiction
design-professional-standards evidence checklist, never auto),
`:risk/screen` (IP/licensing-conflict screening, unconditional-
evaluation discipline, never auto), and `:actuation/release-
deliverable` (POSITIVE, high-stakes -- releasing a real final
deliverable to a client).

### Decision 3: `deliverable-scope-exceeded?` -- the 6th set-containment/subset check

Following `registrar.registry`'s (1st, "sufficiency" polarity),
`casework.registry`'s (2nd), `secondary.registry`'s (3rd),
`consulting.registry`'s (4th, first "permission/boundary" polarity)
and `congregation.registry`'s (5th, second "permission/boundary"
polarity) instances, `design.registry/deliverable-scope-exceeded?`
recomputes `(not (set/subset? deliverable-elements licensed-scope-
elements))` directly from the project's own recorded fields -- the
6th instance overall, and the 3rd in the "permission/boundary"
polarity (a deliverable must stay WITHIN what it is licensed to
contain, not merely satisfy a coverage floor). Gates only
`:actuation/release-deliverable`.

### Decision 4: `ip-licensing-conflict-unresolved-violations` -- the 41st unconditional-evaluation screening grounding, a genuinely new concept

Before writing this check, every prior sibling's governor/registry
namespaces were grepped for `ip-licensing`, `licensing-conflict` and
`intellectual-property` -- zero hits, confirming this is a genuinely
new concept, avoiding the false-precedent-claim risk `leasing`'s
ADR-0001 documents. It is explicitly distinct from `adjustment`/
`intermediation`/`brokerage`/`consulting`'s existing "conflict of
interest" concept, which concerns a professional's divided loyalty
between parties -- `ip-licensing-conflict-unresolved` concerns an
unresolved intellectual-property/licensing clearance dispute on the
deliverable's own content. `ip-licensing-conflict-unresolved-
violations` reuses the unconditional-evaluation DISCIPLINE
(`casualty.governor/sanctions-violations`'s original fix) for the
41st distinct application overall, continuing the count established
across this window's builds (water=25th ... research=40th, design=
41st). Grounded directly in this blueprint's own Trust Control "an
IP/licensing conflict forces a hold, not an override." Gates
`:risk/screen` and `:actuation/release-deliverable`.

### Decision 5: dedicated double-actuation-guard boolean

`:deliverable-released?` is a dedicated boolean on the `project`
record, never a single `:status` value -- the same discipline every
prior sibling governor's guards establish, informed by `cloud-
itonami-isic-6492`'s real status-lifecycle bug (ADR-2607071320).

### Decision 6: Store protocol, MemStore + DatomicStore parity

`design.store/Store` is implemented by both `MemStore` (atom-backed,
default for dev/tests/demo) and `DatomicStore` (`langchain.db`-
backed), proven to satisfy the same contract in `test/design/
store_contract_test.clj` -- the same seam every sibling actor uses so
swapping the SSoT backend is a configuration change, not a rewrite.
The protocol's per-entity accessor is named `project` directly -- not
a Clojure special form, so no `-of` suffix workaround was needed.

### Decision 7: Phase 0→3 rollout

Phase 3's `:auto` set has exactly one member, `:project/intake` (no
capital risk). `:brief/verify` and `:risk/screen` are never auto-
eligible at any phase (matching every sibling's screening-op
posture), and `:actuation/release-deliverable` is permanently
excluded from every phase's `:auto` set -- a structural fact, not a
rollout milestone, enforced by BOTH `design.phase` and `design.
governor`'s `high-stakes` set independently.

### Decision 8: no bespoke domain capability lib

Unlike `banking` (`:banking`/`:swift`) or `research`/`aerospace`/`fab`
(`:cae`/`:eda`), this blueprint's own `:itonami.blueprint/required-
technologies` names no domain-specific capability beyond the generic
robotics/identity/forms/dmn/bpmn/audit-ledger stack -- project
records here are practice-specific rather than a shared cross-
operator data contract, so `design.*` needed no capability-lib
decision to make at all.

### Decision 9: mock + LLM advisor pair

`design.designadvisor` provides `mock-advisor` (deterministic,
default everywhere -- the actor graph and governor contract run
offline) and `llm-advisor` (backed by `langchain.model/ChatModel`,
with a defensive EDN-proposal parser so a malformed LLM response
degrades to a safe low-confidence noop rather than ever auto-
releasing a deliverable).

### Decision 10: no `blueprint.edn` field-sync fixes needed

Matching `advertising`/7310's, `polling`/7320's and `research`/7210's
own experience, this repo's `blueprint.edn` already had the correct
`isic-` prefixed `:id` and correctly populated `:required-
technologies`/`:optional-technologies` matching the `kotoba-lang/
industry` registry's own entry for `"7410"` exactly -- only the
`:maturity` field itself needed adding.

## Alternatives considered

- **A dual-actuation shape** (e.g. adding a separate "issue invoice"
  actuation alongside deliverable release). Rejected: the blueprint's
  own text consistently names only ONE real-world act; inventing a
  second would not be grounded in the blueprint's own text.
- **Merging `deliverable-scope-exceeded?` and `ip-licensing-conflict-
  unresolved` into one check.** Rejected: the former is a ground-truth
  set-containment recompute needing no proposal inspection; the
  latter is an unconditionally-evaluated flag that must also
  HARD-hold the screening op itself on its own finding -- merging them
  would lose the screening op's self-hold property.
- **Reusing the existing "conflict of interest" concept for
  IP/licensing disputes.** Rejected: that concept is about a
  professional's divided loyalty between parties, not an unresolved
  intellectual-property/licensing clearance dispute on a deliverable's
  own content -- a genuinely different real-world failure mode that
  deserves its own named check, confirmed via grep to have zero prior
  instances.

## Consequences

- Fifty-seventh actor in this fleet (56 implemented before this
  build).
- Confirms the set-containment/subset check family generalizes to a
  6th instance, and its "permission/boundary" polarity to a 3rd
  instance.
- Establishes a genuinely NEW unconditional-evaluation-screening
  concept (ip-licensing-conflict-unresolved), grep-verified absent
  from every prior sibling before the claim was finalized.
- `MemStore` ‖ `DatomicStore` parity is proven by `test/design/
  store_contract_test.clj`, the same `:db-api`-driven swap pattern
  every sibling actor uses.
- `blueprint.edn` required no field-sync fixes this time (already
  correct) -- only the `:maturity` flip itself, matching
  `advertising`'s, `polling`'s and `research`'s own experience.
