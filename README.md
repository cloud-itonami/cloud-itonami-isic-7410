# cloud-itonami-isic-7410

Open Business Blueprint for **ISIC Rev.5 7410**: Specialized design
activities.

This repository publishes a design-studio actor -- project intake,
design-professional-standards evidence assessment, IP/licensing-
conflict screening and deliverable release -- as an OSS business that
any qualified, licensed design professional can fork, deploy, run,
improve and sell, so a community or independent studio never
surrenders customer data and ledgers to a closed SaaS.

Built on this workspace's
[`langgraph-clj`](https://github.com/com-junkawasaki/langgraph-clj)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet
([`cloud-itonami-isic-6511`](https://github.com/cloud-itonami/cloud-itonami-isic-6511),
[`6512`](https://github.com/cloud-itonami/cloud-itonami-isic-6512),
[`6621`](https://github.com/cloud-itonami/cloud-itonami-isic-6621),
[`6622`](https://github.com/cloud-itonami/cloud-itonami-isic-6622),
[`6629`](https://github.com/cloud-itonami/cloud-itonami-isic-6629),
[`6520`](https://github.com/cloud-itonami/cloud-itonami-isic-6520),
[`6530`](https://github.com/cloud-itonami/cloud-itonami-isic-6530),
[`6820`](https://github.com/cloud-itonami/cloud-itonami-isic-6820),
[`6612`](https://github.com/cloud-itonami/cloud-itonami-isic-6612),
[`6492`](https://github.com/cloud-itonami/cloud-itonami-isic-6492),
[`6920`](https://github.com/cloud-itonami/cloud-itonami-isic-6920),
[`6611`](https://github.com/cloud-itonami/cloud-itonami-isic-6611),
[`7120`](https://github.com/cloud-itonami/cloud-itonami-isic-7120),
[`8620`](https://github.com/cloud-itonami/cloud-itonami-isic-8620),
[`8530`](https://github.com/cloud-itonami/cloud-itonami-isic-8530),
[`9200`](https://github.com/cloud-itonami/cloud-itonami-isic-9200),
[`7500`](https://github.com/cloud-itonami/cloud-itonami-isic-7500),
[`9603`](https://github.com/cloud-itonami/cloud-itonami-isic-9603),
[`9521`](https://github.com/cloud-itonami/cloud-itonami-isic-9521),
[`9321`](https://github.com/cloud-itonami/cloud-itonami-isic-9321),
[`8730`](https://github.com/cloud-itonami/cloud-itonami-isic-8730),
[`9102`](https://github.com/cloud-itonami/cloud-itonami-isic-9102),
[`9103`](https://github.com/cloud-itonami/cloud-itonami-isic-9103),
[`9602`](https://github.com/cloud-itonami/cloud-itonami-isic-9602),
[`9000`](https://github.com/cloud-itonami/cloud-itonami-isic-9000),
[`8890`](https://github.com/cloud-itonami/cloud-itonami-isic-8890),
[`8610`](https://github.com/cloud-itonami/cloud-itonami-isic-8610),
[`9311`](https://github.com/cloud-itonami/cloud-itonami-isic-9311),
[`8510`](https://github.com/cloud-itonami/cloud-itonami-isic-8510),
[`9412`](https://github.com/cloud-itonami/cloud-itonami-isic-9412),
[`6491`](https://github.com/cloud-itonami/cloud-itonami-isic-6491),
[`8720`](https://github.com/cloud-itonami/cloud-itonami-isic-8720),
[`8521`](https://github.com/cloud-itonami/cloud-itonami-isic-8521),
[`6619`](https://github.com/cloud-itonami/cloud-itonami-isic-6619),
[`3600`](https://github.com/cloud-itonami/cloud-itonami-isic-3600),
[`6190`](https://github.com/cloud-itonami/cloud-itonami-isic-6190),
[`3030`](https://github.com/cloud-itonami/cloud-itonami-isic-3030),
[`3830`](https://github.com/cloud-itonami/cloud-itonami-isic-3830),
[`7020`](https://github.com/cloud-itonami/cloud-itonami-isic-7020),
[`9420`](https://github.com/cloud-itonami/cloud-itonami-isic-9420),
[`9491`](https://github.com/cloud-itonami/cloud-itonami-isic-9491),
[`2610`](https://github.com/cloud-itonami/cloud-itonami-isic-2610),
[`3512`](https://github.com/cloud-itonami/cloud-itonami-isic-3512),
[`8810`](https://github.com/cloud-itonami/cloud-itonami-isic-8810),
[`8691`](https://github.com/cloud-itonami/cloud-itonami-isic-8691),
[`8569`](https://github.com/cloud-itonami/cloud-itonami-isic-8569),
[`6419`](https://github.com/cloud-itonami/cloud-itonami-isic-6419),
[`7310`](https://github.com/cloud-itonami/cloud-itonami-isic-7310),
[`7320`](https://github.com/cloud-itonami/cloud-itonami-isic-7320),
[`7210`](https://github.com/cloud-itonami/cloud-itonami-isic-7210)) --
here it is **Designer-LLM ⊣ Design Delivery Governor**.

> **Why an actor layer at all?** An LLM is great at drafting a
> project-intake summary, normalizing records, and checking whether a
> project's own deliverable elements actually stay within its own
> recorded licensed scope -- but it has **no notion of which
> jurisdiction's design-professional-standards/IP law is official, no
> license to release a real final deliverable, and no way to know on
> its own whether an IP/licensing conflict against a project has
> actually stayed unresolved**. Letting it release a deliverable
> directly invites fabricated regulatory citations, a deliverable
> that includes elements beyond its own licensed scope, and an
> unresolved IP dispute being quietly shipped to a client -- and
> liability, and infringement risk, for whoever runs it. This project
> seals the Designer-LLM into a single node and wraps it with an
> independent **Design Delivery Governor**, a human **approval
> workflow**, and an immutable **audit ledger**.

## Scope: what this actor does and does not do

This actor covers project intake through design-professional-
standards evidence assessment, IP/licensing-conflict screening and
deliverable release. It does **not**, by itself, hold any
professional license required to operate as a design studio in a
given jurisdiction, and it does not claim to. It also does **not**
create the creative work itself, or judge the aesthetic/creative
merit of a design -- `design.registry/deliverable-scope-exceeded?` is
a pure set-containment recompute against the project's own recorded
fields, not a creative review. Whoever deploys and operates a live
instance (a licensed design studio) supplies any jurisdiction-specific
license, the real creative work and the real design-management/
asset-licensing integrations, and bears that jurisdiction's liability
-- the software supplies the governed, spec-cited, audited execution
scaffold so that studio does not have to build the compliance layer
from scratch.

### Actuation

**Releasing a real final deliverable to a client is never autonomous,
at any phase, by construction.** Two independent layers enforce this
(`design.governor`'s `:actuation/release-deliverable` high-stakes
gate and `design.phase`'s phase table, which never puts `:actuation/
release-deliverable` in any phase's `:auto` set) -- see `design.
phase`'s docstring and `test/design/phase_test.clj`'s `release-
deliverable-never-auto-at-any-phase`. The actor may draft, check and
recommend; a human studio operator is always the one who actually
releases a deliverable. Matching `leasing`'s/`underwriting`'s/
`testlab`'s/`clinic`'s/`veterinary`'s/`funeral`'s/`parksafety`'s/
`salon`'s/`entertainment`'s/`facility`'s/`consulting`'s/
`advertising`'s/`polling`'s/`research`'s single-actuation shape,
grounded directly in this blueprint's own README text ("No automated
proposal, by itself, can complete the following without governor
approval and audit evidence: releasing a final deliverable to a
client") -- a POSITIVE actuation (releasing a real record), matching
this fleet's majority actuation shape (`3600`/`6190` are the fleet's
two NEGATIVE-actuation exceptions).

## The core contract

```
project intake + jurisdiction facts (design.facts, spec-cited)
        |
        v
   ┌──────────────┐   proposal      ┌───────────────────────┐
   │ Designer-LLM │ ─────────────▶ │ Design Delivery                │  (independent system)
   │ (sealed)     │  + citations    │ Governor:                    │
   └──────────────┘                 │ spec-basis · evidence-       │
          │                 commit ◀┼ incomplete · deliverable-      │
          │                         │ scope-exceeded (subset) ·      │
    record + ledger        escalate ┼ ip-licensing-conflict-          │
          │              (ALWAYS for│ unresolved (unconditional) ·    │
          │               :actuation│ already-released                │
          │               /release- └───────────────────────┘
          ▼               deliverable)
      human approval
```

**The Designer-LLM never releases a deliverable the Design Delivery
Governor would reject, and never does so without a human sign-off.**
Hard violations (fabricated regulatory requirements; unsupported
evidence; a deliverable exceeding its own licensed scope; an
unresolved IP/licensing conflict; a double release) force **hold**
and *cannot* be approved past; a clean release proposal still always
routes to a human.

## Run

```bash
clojure -M:dev:run     # walk one clean single-actuation lifecycle + four HARD-hold cases through the actor
clojure -M:dev:test    # governor contract · phase invariants · store parity · registry conformance · facts coverage
clojure -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here a fabrication/prototyping
robot produces physical design mockups/prototypes, under the actor,
gated by the independent **Design Delivery Governor**. The governor
never dispatches hardware itself; `:high`/`:safety-critical` actions
require human sign-off.

## Open business

This repository is not only source code. It is a public, forkable
business model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, Design Delivery Governor, deliverable-release draft records, audit ledger |
| Business blueprint | Customer, offer, pricing, unit economics, sales motion |
| Operator playbook | How to fork, license, deploy and support the service in a jurisdiction |
| Trust controls | Governance, security reporting, actuation invariant, audit requirements |

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md) to start this as an
open business on itonami.cloud, and
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) for the
full architecture and decision record.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`7410`). This vertical's project records are practice-specific rather
than a shared cross-operator data contract, so `design.*` runs on the
generic robotics/identity/forms/dmn/bpmn/audit-ledger stack only -- no
bespoke domain capability lib to reference at all.

## Layout

| File | Role |
|---|---|
| `src/design/store.cljc` | **Store** protocol -- `MemStore` ‖ `DatomicStore` (`langchain.db`) + append-only audit ledger + deliverable-release history. No dynamically-filed sub-record -- the actuation op acts directly on a pre-seeded project, and the double-actuation guard checks a dedicated `:deliverable-released?` boolean rather than a `:status` value |
| `src/design/registry.cljc` | Deliverable-release draft records, plus `deliverable-scope-exceeded?` -- the SIXTH instance of this fleet's set-containment/subset check family (`registrar`/`casework`/`secondary` established the first three, `consulting`/`congregation` the fourth and fifth in the 'permission/boundary' polarity) |
| `src/design/facts.cljc` | Per-jurisdiction design-professional-standards/IP catalog with an official spec-basis citation per entry, honest coverage reporting |
| `src/design/designadvisor.cljc` | **Designer-LLM** -- `mock-advisor` ‖ `llm-advisor`; intake/brief-verification/IP-licensing-conflict-screening/deliverable-release proposals |
| `src/design/governor.cljc` | **Design Delivery Governor** -- 3 HARD checks (spec-basis · evidence-incomplete · deliverable-scope-exceeded, pure ground-truth subset recompute · ip-licensing-conflict-unresolved, unconditional evaluation, the FORTY-FIRST grounding of this discipline, a genuinely new concept distinct from this fleet's existing 'conflict of interest' concept, grounded in this blueprint's own Trust Control text) + already-released guard + 1 soft (confidence/actuation gate) |
| `src/design/phase.cljc` | **Phase 0→3** -- read-only → assisted intake → assisted verify → supervised (deliverable release always human; project intake is the ONLY auto-eligible op, no direct capital risk) |
| `src/design/operation.cljc` | **OperationActor** -- langgraph-clj StateGraph |
| `src/design/sim.cljc` | demo driver |
| `test/design/*_test.clj` | governor contract · phase invariants · store parity · registry conformance · facts coverage |

## Business-process coverage (honest)

This actor covers project intake through design-professional-
standards evidence assessment, IP/licensing-conflict screening and
deliverable release -- the core governed lifecycle this blueprint's
own `docs/business-model.md` names as its Offer:

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Project intake + per-jurisdiction design-professional-standards checklisting, HARD-gated on an official spec-basis citation (`:project/intake`/`:brief/verify`) | Real design-management/asset-licensing integration, real creative production itself (see `design.facts`'s docstring) |
| IP/licensing-conflict screening, evaluated unconditionally so the screening op itself can HARD-hold on its own finding (`:risk/screen`) | Any aesthetic/creative judgment itself -- deliberately outside this actor's competence |
| Deliverable release, HARD-gated on full evidence and the project's own licensed-scope subset, plus a double-release guard (`:actuation/release-deliverable`) | |
| Immutable audit ledger for every intake/verification/screening/release decision | |

Extending coverage is additive: add the next gate (e.g. a trademark-
clearance-search check) as its own governed op with its own HARD
checks and tests, following the SAME "an independent governor
re-verifies against the actor's own records before any real-world act"
pattern this repo's flagship op already establishes.

## Jurisdiction coverage (honest)

`design.facts/coverage` reports how many requested jurisdictions
actually have an official spec-basis in `design.facts/catalog` --
currently 4 seeded (JPN, USA, GBR, DEU) out of ~194 jurisdictions
worldwide. This is a starting catalog to prove the governor contract
end-to-end, not a claim of global coverage. Adding a jurisdiction is
additive: one map entry in `design.facts/catalog`, citing a real
official source -- never fabricate a jurisdiction's requirements to
make coverage look bigger.

## Maturity

`:implemented` -- `Designer-LLM` + `Design Delivery Governor` run as
real, tested code (see `Run` above), promoted from the originally-
published `:blueprint`-tier scaffold, modeled closely on the fifty-
six prior actors' architecture. See `docs/adr/0001-architecture.md`
for the history and design.

## License

Code and implementation templates are AGPL-3.0-or-later.
