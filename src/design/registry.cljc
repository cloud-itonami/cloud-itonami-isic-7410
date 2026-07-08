(ns design.registry
  "Pure-function deliverable-release record construction -- an
  append-only design-studio book-of-record draft.

  Like every sibling actor's registry, there is no single
  international check-digit standard for a deliverable-release
  reference number -- every studio/jurisdiction assigns its own
  reference format. This namespace does NOT invent one; it builds a
  jurisdiction-scoped sequence number and validates the record's
  required fields, the same honest, non-fabricating discipline
  `design.facts` uses.

  `deliverable-scope-exceeded?` is the SIXTH instance of this fleet's
  set-containment/subset check family (`registrar.registry/
  prerequisites-satisfied?`/`casework.registry/eligibility-criteria-
  unsatisfied?`/`secondary.registry/graduation-requirements-
  unsatisfied?` established the first three, `consulting.registry/
  engagement-scope-exceeded?` the fourth in the 'permission/boundary'
  polarity, `congregation.registry/doctrinal-statement-exceeds-core-
  doctrine?` the fifth), applying the SAME `clojure.set/subset?`
  mechanism -- a project's own recorded `:deliverable-elements` set
  must stay a subset of its own recorded `:licensed-scope-elements`
  set -- the THIRD instance of the 'permission/boundary' polarity, a
  direct, natural mapping onto real design-studio IP/licensing
  practice (a deliverable that includes elements -- fonts, stock
  assets, trademarked motifs -- beyond what the engagement's own
  licensing clearance covers is exactly the failure mode a studio
  operator must not let an advisor wave through).

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real design-management/asset-licensing system. It
  builds the RECORD a design studio would keep, not the act of
  releasing the deliverable itself (that is `design.operation`'s
  `:actuation/release-deliverable`, always human-gated -- see README
  `Actuation`)."
  (:require [clojure.set :as set]
            [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is
  the studio's own act, not this actor's. See README `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn deliverable-scope-exceeded?
  "Does `project`'s own `:deliverable-elements` set contain any
  element NOT in its own `:licensed-scope-elements` set? A pure
  ground-truth check against the project's own permanent fields -- no
  upstream comparison needed. The SIXTH instance of this fleet's set-
  containment/subset check family (see ns docstring)."
  [{:keys [deliverable-elements licensed-scope-elements]}]
  (and (set? deliverable-elements) (set? licensed-scope-elements)
       (not (set/subset? deliverable-elements licensed-scope-elements))))

(defn register-deliverable-release
  "Validate + construct the DELIVERABLE-RELEASE registration DRAFT --
  the studio's own act of releasing a real final deliverable to a
  client. Pure function -- does not touch any real design-management
  system; it builds the RECORD a studio would keep. `design.governor`
  independently re-verifies the project's own licensed-scope
  sufficiency and IP/licensing-conflict resolution status, and blocks
  a double-release for the same project, before this is ever allowed
  to commit."
  [project-id jurisdiction sequence]
  (when-not (and project-id (not= project-id ""))
    (throw (ex-info "deliverable-release: project_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "deliverable-release: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "deliverable-release: sequence must be >= 0" {})))
  (let [release-number (str (str/upper-case jurisdiction) "-REL-" (zero-pad sequence 6))
        record {"record_id" release-number
                "kind" "deliverable-release-draft"
                "project_id" project-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "release_number" release-number
     "certificate" (unsigned-certificate "DeliverableRelease" release-number release-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
