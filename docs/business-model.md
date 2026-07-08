# Business Model: Specialized design activities

## Classification

- Repository: `cloud-itonami-isic-7410`
- ISIC Rev.5: `7410`
- Activity: specialized design activities -- fashion, industrial, graphic and interior design services for clients
- Social impact: professional standards, data sovereignty, transparent audit

## Customer

- independent design studios
- cooperative design collectives
- community design-access programs

## Offer

- brief intake
- concept/design proposal
- final-deliverable proposal
- immutable audit ledger

## Revenue

- self-host setup: one-time implementation fee
- managed hosting: monthly subscription per studio
- support: monthly retainer with SLA
- migration: import from an incumbent design-management system
- per-project fee

## Trust Controls

- no final deliverable is released to a client without human sign-off
- an IP/licensing conflict forces a hold, not an override
- every deliverable path is auditable
- emergency manual override paths remain outside LLM control
- a fabricated jurisdiction citation, incomplete evidence, or a deliverable
  whose own elements exceed its own recorded licensed scope -- each forces
  a hold, not an override
- deliverable release is logged and escalated, and cannot be finalized
  twice for the same project: a double-release attempt is held off this
  actor's own project facts alone, with no upstream comparison needed

## Design Delivery Governor: decision rule

`blueprint.edn` fixes `:itonami.blueprint/governor` to `:design-
delivery-governor` -- this is not a generic "review step," it is the
one gate the ONE real-world act this business performs (releasing a
final deliverable to a client) must pass. The governor sits between
the Designer-LLM and execution, per the README's Core Contract:

```text
Designer-LLM -> Design Delivery Governor -> hold, proceed, or human approval
```

**Approves**: routine design actions proposed against a project that
already has a consented brief on file, deliverable elements within
its own licensed scope, and no unresolved IP/licensing conflict.
These proceed straight to the engagement ledger.

**Rejects or escalates**: the governor refuses to let the advisor
release a deliverable on its own authority when any of the following
hold -- a fabricated jurisdiction spec-basis; incomplete evidence; a
deliverable whose own elements exceed its own recorded licensed
scope; an unresolved IP/licensing conflict. A clean release proposal
still always routes to a human -- `:actuation/release-deliverable` is
never auto-committed, at any rollout phase.
