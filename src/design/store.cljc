(ns design.store
  "SSoT for the design actor, behind a `Store` protocol so the backend
  is a swap, not a rewrite -- the same seam every prior `cloud-
  itonami-isic-*` actor in this fleet uses:

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/design/store_contract_test.clj), which is the whole point: the
  actor, the Design Delivery Governor and the audit ledger never know
  which SSoT they run on.

  Like `leasing`/`underwriting`/`testlab`/`clinic`/`veterinary`/
  `funeral`/`parksafety`/`salon`/`entertainment`/`facility`/
  `consulting`/`advertising`/`polling`/`research`, this actor has ONE
  actuation event (releasing a real final deliverable to a client)
  acting on a `project` entity, with its OWN history collection,
  sequence counter and dedicated double-actuation-guard boolean
  (`:deliverable-released?`, never a `:status` value) -- the same
  discipline every prior sibling governor's guards establish, informed
  by `cloud-itonami-isic-6492`'s status-lifecycle bug
  (ADR-2607071320).

  The ledger stays append-only on every backend: 'which project was
  screened for an unresolved IP/licensing conflict, which deliverable
  was released, on what jurisdictional basis, approved by whom' is
  always a query over an immutable log -- the audit trail a client
  trusting a design studio needs, and the evidence a studio needs if a
  release decision is later disputed."
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [design.registry :as registry]
            [langchain.db :as d]))

(defprotocol Store
  (project [s id])
  (all-projects [s])
  (risk-screen-of [s project-id] "committed IP/licensing-conflict screening verdict for a project, or nil")
  (brief-of [s project-id] "committed brief evidence assessment, or nil")
  (ledger [s])
  (release-history [s] "the append-only deliverable-release history (design.registry drafts)")
  (next-release-sequence [s jurisdiction] "next release-number sequence for a jurisdiction")
  (project-already-released? [s project-id] "has this project's deliverable already been released?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-projects [s projects] "replace/seed the project directory (map id->project)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained project set covering the actuation
  lifecycle (releasing a deliverable) so the actor + tests run
  offline."
  []
  {:projects
   {"project-1" {:id "project-1" :client-name "Sato Apparel"
                :deliverable-elements #{:logo-mark}
                :licensed-scope-elements #{:logo-mark :packaging-motif}
                :ip-licensing-conflict-unresolved? false
                :deliverable-released? false
                :jurisdiction "JPN" :status :intake}
    "project-2" {:id "project-2" :client-name "Atlantis Interiors"
                :deliverable-elements #{:logo-mark}
                :licensed-scope-elements #{:logo-mark :packaging-motif}
                :ip-licensing-conflict-unresolved? false
                :deliverable-released? false
                :jurisdiction "ATL" :status :intake}
    "project-3" {:id "project-3" :client-name "鈴木家具工房"
                :deliverable-elements #{:logo-mark :stock-photo-set}
                :licensed-scope-elements #{:logo-mark :packaging-motif}
                :ip-licensing-conflict-unresolved? false
                :deliverable-released? false
                :jurisdiction "JPN" :status :intake}
    "project-4" {:id "project-4" :client-name "田中出版デザイン"
                :deliverable-elements #{:logo-mark}
                :licensed-scope-elements #{:logo-mark :packaging-motif}
                :ip-licensing-conflict-unresolved? true
                :deliverable-released? false
                :jurisdiction "JPN" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- release-deliverable!
  "Backend-agnostic `:project/mark-released` -- looks up the project
  via the protocol and drafts the deliverable-release record, and
  returns {:result .. :project-patch ..} for the caller to persist."
  [s project-id]
  (let [p (project s project-id)
        seq-n (next-release-sequence s (:jurisdiction p))
        result (registry/register-deliverable-release project-id (:jurisdiction p) seq-n)]
    {:result result
     :project-patch {:deliverable-released? true
                    :release-number (get result "release_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (project [_ id] (get-in @a [:projects id]))
  (all-projects [_] (sort-by :id (vals (:projects @a))))
  (risk-screen-of [_ id] (get-in @a [:risk-screens id]))
  (brief-of [_ project-id] (get-in @a [:briefs project-id]))
  (ledger [_] (:ledger @a))
  (release-history [_] (:releases @a))
  (next-release-sequence [_ jurisdiction] (get-in @a [:release-sequences jurisdiction] 0))
  (project-already-released? [_ project-id] (boolean (get-in @a [:projects project-id :deliverable-released?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :project/upsert
      (swap! a update-in [:projects (:id value)] merge value)

      :brief/set
      (swap! a assoc-in [:briefs (first path)] payload)

      :risk-screen/set
      (swap! a assoc-in [:risk-screens (first path)] payload)

      :project/mark-released
      (let [project-id (first path)
            {:keys [result project-patch]} (release-deliverable! s project-id)
            jurisdiction (:jurisdiction (project s project-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:release-sequences jurisdiction] (fnil inc 0))
                       (update-in [:projects project-id] merge project-patch)
                       (update :releases registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-projects [s projects] (when (seq projects) (swap! a assoc :projects projects)) s))

(defn seed-db
  "A MemStore seeded with the demo project set. The deterministic
  default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :briefs {} :risk-screens {} :ledger [] :release-sequences {}
                           :releases []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Compound values (brief/risk-screen payloads, ledger facts, release
  records, and the project's own element sets) are stored as EDN
  strings so `langchain.db` doesn't expand them into sub-entities --
  the same convention every sibling actor's store uses."
  {:project/id                          {:db/unique :db.unique/identity}
   :brief/project-id                    {:db/unique :db.unique/identity}
   :risk-screen/project-id              {:db/unique :db.unique/identity}
   :ledger/seq                         {:db/unique :db.unique/identity}
   :release/seq                        {:db/unique :db.unique/identity}
   :release-sequence/jurisdiction      {:db/unique :db.unique/identity}})

(defn- enc [v] (pr-str v))
(defn- dec* [s] (when s (edn/read-string s)))

(defn- project->tx [{:keys [id client-name deliverable-elements licensed-scope-elements
                          ip-licensing-conflict-unresolved?
                          deliverable-released?
                          jurisdiction status release-number]}]
  (cond-> {:project/id id}
    client-name                                    (assoc :project/client-name client-name)
    deliverable-elements                           (assoc :project/deliverable-elements (enc deliverable-elements))
    licensed-scope-elements                        (assoc :project/licensed-scope-elements (enc licensed-scope-elements))
    (some? ip-licensing-conflict-unresolved?)      (assoc :project/ip-licensing-conflict-unresolved? ip-licensing-conflict-unresolved?)
    (some? deliverable-released?)                  (assoc :project/deliverable-released? deliverable-released?)
    jurisdiction                                     (assoc :project/jurisdiction jurisdiction)
    status                                           (assoc :project/status status)
    release-number                                   (assoc :project/release-number release-number)))

(def ^:private project-pull
  [:project/id :project/client-name :project/deliverable-elements :project/licensed-scope-elements
   :project/ip-licensing-conflict-unresolved? :project/deliverable-released?
   :project/jurisdiction :project/status :project/release-number])

(defn- pull->project [m]
  (when (:project/id m)
    {:id (:project/id m) :client-name (:project/client-name m)
     :deliverable-elements (dec* (:project/deliverable-elements m))
     :licensed-scope-elements (dec* (:project/licensed-scope-elements m))
     :ip-licensing-conflict-unresolved? (boolean (:project/ip-licensing-conflict-unresolved? m))
     :deliverable-released? (boolean (:project/deliverable-released? m))
     :jurisdiction (:project/jurisdiction m) :status (:project/status m)
     :release-number (:project/release-number m)}))

(defrecord DatomicStore [conn]
  Store
  (project [_ id]
    (pull->project (d/pull (d/db conn) project-pull [:project/id id])))
  (all-projects [_]
    (->> (d/q '[:find [?id ...] :where [?e :project/id ?id]] (d/db conn))
         (map #(pull->project (d/pull (d/db conn) project-pull [:project/id %])))
         (sort-by :id)))
  (risk-screen-of [_ id]
    (dec* (d/q '[:find ?p . :in $ ?pid
                :where [?k :risk-screen/project-id ?pid] [?k :risk-screen/payload ?p]]
              (d/db conn) id)))
  (brief-of [_ project-id]
    (dec* (d/q '[:find ?p . :in $ ?pid
                :where [?a :brief/project-id ?pid] [?a :brief/payload ?p]]
              (d/db conn) project-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (release-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :release/seq ?s] [?e :release/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (next-release-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :release-sequence/jurisdiction ?j] [?e :release-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (project-already-released? [s project-id]
    (boolean (:deliverable-released? (project s project-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :project/upsert
      (d/transact! conn [(project->tx value)])

      :brief/set
      (d/transact! conn [{:brief/project-id (first path) :brief/payload (enc payload)}])

      :risk-screen/set
      (d/transact! conn [{:risk-screen/project-id (first path) :risk-screen/payload (enc payload)}])

      :project/mark-released
      (let [project-id (first path)
            {:keys [result project-patch]} (release-deliverable! s project-id)
            jurisdiction (:jurisdiction (project s project-id))
            next-n (inc (next-release-sequence s jurisdiction))]
        (d/transact! conn
                     [(project->tx (assoc project-patch :id project-id))
                      {:release-sequence/jurisdiction jurisdiction :release-sequence/next next-n}
                      {:release/seq (count (release-history s)) :release/record (enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (enc fact)}])
    fact)
  (with-projects [s projects]
    (when (seq projects) (d/transact! conn (mapv project->tx (vals projects)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:projects ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [projects]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-projects s projects))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo project set -- the Datomic-
  backed analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
