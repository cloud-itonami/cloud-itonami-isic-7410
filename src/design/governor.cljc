(ns design.governor
  "Design Delivery Governor -- the independent compliance layer that
  earns the Designer-LLM the right to commit. The LLM has no notion of
  design-professional-standards/IP law, whether a project's own
  proposed deliverable elements actually stay within its own licensed
  scope, whether an IP/licensing conflict against a project has
  actually stayed unresolved, or when an act stops being a draft and
  becomes a real-world deliverable release to a client, so this MUST
  be a separate system able to *reject* a proposal and fall back to
  HOLD -- the design-studio analog of `cloud-itonami-isic-6512`'s
  CasualtyGovernor.

  Four checks, in priority order, ALL HARD violations: a human
  approver CANNOT override them (you don't get to approve your way
  past a fabricated jurisdiction spec-basis, incomplete evidence, a
  deliverable that exceeds its own licensed scope, or an unresolved
  IP/licensing conflict). The confidence/actuation gate is SOFT: it
  asks a human to look (low confidence / actuation), and the human may
  approve -- but see `design.phase`: for `:stake :actuation/release-
  deliverable` (a real client-facing act) NO phase ever allows auto-
  commit either. Two independent layers agree that actuation is
  always a human call.

    1. Spec-basis                  -- did the brief proposal cite an
                                       OFFICIAL source (`design.
                                       facts`), or invent one?
    2. Evidence incomplete         -- for `:actuation/release-
                                       deliverable`, has the project
                                       actually been assessed with a
                                       full design-brief-record/
                                       concept-approval-record/
                                       licensing-clearance-record/ip-
                                       assignment-record evidence
                                       checklist on file?
    3. Deliverable scope exceeded  -- for `:actuation/release-
                                       deliverable`, INDEPENDENTLY
                                       recompute whether the project's
                                       own deliverable elements stay
                                       within its own licensed-scope
                                       elements (`design.registry/
                                       deliverable-scope-exceeded?`)
                                       -- needs no proposal inspection
                                       or stored-verdict lookup at
                                       all. The SIXTH instance of this
                                       fleet's set-containment/subset
                                       check family (`registrar.
                                       governor/prerequisites-not-
                                       satisfied-violations`/
                                       `casework.governor/eligibility-
                                       criteria-unsatisfied-
                                       violations`/`secondary.
                                       governor/graduation-
                                       requirements-unsatisfied-
                                       violations` established the
                                       first three, `consulting.
                                       governor/engagement-scope-
                                       exceeded-violations` the
                                       fourth in the 'permission/
                                       boundary' polarity,
                                       `congregation.governor/
                                       doctrinal-statement-exceeds-
                                       core-doctrine-violations` the
                                       fifth), applying the SAME
                                       'permission/boundary' polarity
                                       for the THIRD time.
    4. IP/licensing conflict
       unresolved                     -- reported by THIS proposal
                                       itself (a `:risk/screen` that
                                       just found one), or already on
                                       file for the project (`:risk/
                                       screen`/`:actuation/release-
                                       deliverable`). Evaluated
                                       UNCONDITIONALLY (not scoped to
                                       a specific op), the SAME
                                       discipline `casualty.governor/
                                       sanctions-violations`/...
                                       (forty prior siblings, most
                                       recently `research.governor/
                                       data-reproducibility-risk-
                                       unresolved-violations`)...
                                       established -- the FORTY-
                                       FIRST distinct application of
                                       this exact discipline overall,
                                       and a genuinely NEW concept
                                       (grep-verified absent from
                                       every prior sibling's check
                                       names before this claim was
                                       finalized -- distinct from
                                       `adjustment`/`intermediation`/
                                       `brokerage`/`consulting`'s
                                       'conflict of interest' concept,
                                       which is about a PROFESSIONAL's
                                       divided loyalty, not an IP/
                                       licensing-clearance conflict on
                                       a deliverable's own content),
                                       grounded directly in this
                                       blueprint's own Trust Control
                                       'an IP/licensing conflict forces
                                       a hold, not an override'.
    5. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:actuation/
                                       release-deliverable` (a REAL
                                       client-facing act) -> escalate.

  One more guard, double-release prevention, is enforced but NOT
  listed as a numbered HARD check above because it needs no upstream
  comparison at all -- `already-released-violations` refuses to
  release a deliverable for the SAME project twice, off a dedicated
  `:deliverable-released?` fact (never a `:status` value) -- the SAME
  'check a dedicated boolean, not status' discipline every prior
  sibling governor's guards establish, informed by `cloud-itonami-
  isic-6492`'s status-lifecycle bug (ADR-2607071320)."
  (:require [design.facts :as facts]
            [design.registry :as registry]
            [design.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Releasing a real final deliverable to a client is the ONE real-world
  actuation event this actor performs -- a single-member set, matching
  `leasing`'s/`underwriting`'s/`testlab`'s/`clinic`'s/`veterinary`'s/
  `funeral`'s/`parksafety`'s/`salon`'s/`entertainment`'s/`facility`'s/
  `consulting`'s/`advertising`'s/`polling`'s/`research`'s single-
  actuation shape, grounded directly in this blueprint's own README
  ('No automated proposal, by itself, can complete the following
  without governor approval and audit evidence: releasing a final
  deliverable to a client')."
  #{:actuation/release-deliverable})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:brief/verify` (or `:actuation/release-deliverable`) proposal
  with no spec-basis citation is a HARD violation -- never invent a
  jurisdiction's design-professional-standards requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:brief/verify :actuation/release-deliverable} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案はデザイン専門職基準として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:actuation/release-deliverable`, the jurisdiction's required
  design-brief-record/concept-approval-record/licensing-clearance-
  record/ip-assignment-record evidence must actually be satisfied --
  do not trust the advisor's self-reported confidence alone."
  [{:keys [op subject]} st]
  (when (= op :actuation/release-deliverable)
    (let [p (store/project st subject)
          brief (store/brief-of st subject)]
      (when-not (and brief
                     (facts/required-evidence-satisfied?
                      (:jurisdiction p) (:checklist brief)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(デザインブリーフ記録/コンセプト承認記録/ライセンスクリアランス記録/知的財産権譲渡記録等)が充足していない状態での提案"}]))))

(defn- deliverable-scope-exceeded-violations
  "For `:actuation/release-deliverable`, INDEPENDENTLY recompute
  whether the project's own deliverable elements stay within its own
  licensed-scope elements via `design.registry/deliverable-scope-
  exceeded?` -- needs no proposal inspection or stored-verdict lookup
  at all, since its inputs are permanent ground-truth fields already
  on the project."
  [{:keys [op subject]} st]
  (when (= op :actuation/release-deliverable)
    (let [p (store/project st subject)]
      (when (registry/deliverable-scope-exceeded? p)
        [{:rule :deliverable-scope-exceeded
          :detail (str subject " の納品要素(" (:deliverable-elements p)
                      ")がライセンス範囲(" (:licensed-scope-elements p) ")を超過")}]))))

(defn- ip-licensing-conflict-unresolved-violations
  "An unresolved IP/licensing conflict -- reported by THIS proposal
  (e.g. a `:risk/screen` that itself just found one), or already on
  file in the store for the project (`:risk/screen`/`:actuation/
  release-deliverable`) -- is a HARD, un-overridable hold. Evaluated
  UNCONDITIONALLY (not scoped to a specific op) so the screening op
  itself can HARD-hold on its own finding."
  [{:keys [op subject]} proposal st]
  (let [hit-in-proposal? (= :unresolved (get-in proposal [:value :verdict]))
        project-id (when (contains? #{:risk/screen :actuation/release-deliverable} op) subject)
        hit-on-file? (and project-id (= :unresolved (:verdict (store/risk-screen-of st project-id))))]
    (when (or hit-in-proposal? hit-on-file?)
      [{:rule :ip-licensing-conflict-unresolved
        :detail "未解決の知的財産権/ライセンス紛争がある案件の成果物引渡し提案は進められない"}])))

(defn- already-released-violations
  "For `:actuation/release-deliverable`, refuses to release a
  deliverable for the SAME project twice, off a dedicated
  `:deliverable-released?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/release-deliverable)
    (when (store/project-already-released? st subject)
      [{:rule :already-released
        :detail (str subject " は既に成果物引渡し済み")}])))

(defn check
  "Censors a Designer-LLM proposal against the governor rules. Returns
  {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (deliverable-scope-exceeded-violations request st)
                           (ip-licensing-conflict-unresolved-violations request proposal st)
                           (already-released-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
