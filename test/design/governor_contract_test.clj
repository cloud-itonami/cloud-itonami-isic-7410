(ns design.governor-contract-test
  "The governor contract as executable tests -- the design analog of
  `cloud-itonami-isic-6512`'s `casualty.governor-contract-test`. The
  single invariant under test:

    Designer-LLM never releases a deliverable the Design Delivery
    Governor would reject, `:actuation/release-deliverable` NEVER
    auto-commits at any phase, `:project/intake` (no direct capital
    risk) MAY auto-commit when clean, and every decision (commit OR
    hold) leaves exactly one ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [design.store :as store]
            [design.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :studio-operator :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- verify!
  "Walks `subject` through verify -> approve, leaving a brief
  assessment on file. Uses distinct thread-ids per call site by
  suffixing `tid-prefix`."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-verify") {:op :brief/verify :subject subject} operator)
  (approve! actor (str tid-prefix "-verify")))

(deftest clean-intake-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :project/intake :subject "project-1"
                   :patch {:id "project-1" :client-name "Sato Apparel"}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= "Sato Apparel" (:client-name (store/project db "project-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))))

(deftest brief-verify-always-needs-approval
  (testing "verify is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :brief/verify :subject "project-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/brief-of db "project-1")))))))

(deftest fabricated-jurisdiction-is-held
  (testing "a brief/verify proposal with no official spec-basis -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :brief/verify :subject "project-1" :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/brief-of db "project-1")) "no brief assessment written"))))

(deftest release-deliverable-without-brief-is-held
  (testing "actuation/release-deliverable before any brief verification -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :actuation/release-deliverable :subject "project-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest deliverable-scope-exceeded-is-held
  (testing "a project whose own deliverable elements exceed its own licensed scope -> HOLD"
    (let [[db actor] (fresh)
          _ (verify! actor "t5pre" "project-3")
          res (exec-op actor "t5" {:op :actuation/release-deliverable :subject "project-3"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:deliverable-scope-exceeded} (-> (store/ledger db) last :basis)))
      (is (empty? (store/release-history db))))))

(deftest ip-licensing-conflict-is-held-and-unoverridable
  (testing "an unresolved IP/licensing conflict on a project -> HOLD, and never reaches request-approval -- exercised via :risk/screen DIRECTLY, not via the actuation op against an unscreened project (see this actor's governor ns docstring / parksafety's ADR-2607071922 Decision 5 / eldercare's, museum's, conservation's, salon's, entertainment's, casework's, hospital's, facility's, school's, association's, leasing's, behavioral's, secondary's, card's, water's, telecom's, aerospace's, recovery's, consulting's, union's, congregation's, fab's, energy's, care's, navigator's, learning's, banking's, advertising's, polling's and research's ADR-0001s)"
    (let [[db actor] (fresh)
          res (exec-op actor "t6" {:op :risk/screen :subject "project-4"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:ip-licensing-conflict-unresolved} (-> (store/ledger db) first :basis)))
      (is (nil? (store/risk-screen-of db "project-4")) "no clearance written"))))

(deftest release-deliverable-always-escalates-then-human-decides
  (testing "a clean, fully-assessed project still ALWAYS interrupts for human approval -- actuation/release-deliverable is never auto"
    (let [[db actor] (fresh)
          _ (verify! actor "t7pre" "project-1")
          r1 (exec-op actor "t7" {:op :actuation/release-deliverable :subject "project-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, release record drafted"
        (let [r2 (approve! actor "t7")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:deliverable-released? (store/project db "project-1"))))
          (is (= 1 (count (store/release-history db))) "one draft release record"))))))

(deftest release-deliverable-double-release-is-held
  (testing "releasing the same project's deliverable twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (verify! actor "t8pre" "project-1")
          _ (exec-op actor "t8a" {:op :actuation/release-deliverable :subject "project-1"} operator)
          _ (approve! actor "t8a")
          res (exec-op actor "t8" {:op :actuation/release-deliverable :subject "project-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-released} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/release-history db))) "still only the one earlier release"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :project/intake :subject "project-1"
                          :patch {:id "project-1" :client-name "Sato Apparel"}} operator)
      (exec-op actor "b" {:op :brief/verify :subject "project-1" :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
