(ns design.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a
  configuration change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the
  sibling actor."
  (:require [clojure.test :refer [deftest is testing]]
            [design.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "Sato Apparel" (:client-name (store/project s "project-1"))))
      (is (= "JPN" (:jurisdiction (store/project s "project-1"))))
      (is (= #{:logo-mark} (:deliverable-elements (store/project s "project-1"))))
      (is (= #{:logo-mark :packaging-motif} (:licensed-scope-elements (store/project s "project-1"))))
      (is (false? (:ip-licensing-conflict-unresolved? (store/project s "project-1"))))
      (is (= #{:logo-mark :stock-photo-set} (:deliverable-elements (store/project s "project-3"))))
      (is (true? (:ip-licensing-conflict-unresolved? (store/project s "project-4"))))
      (is (false? (:deliverable-released? (store/project s "project-1"))))
      (is (= ["project-1" "project-2" "project-3" "project-4"]
             (mapv :id (store/all-projects s))))
      (is (nil? (store/risk-screen-of s "project-1")))
      (is (nil? (store/brief-of s "project-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/release-history s)))
      (is (zero? (store/next-release-sequence s "JPN")))
      (is (false? (store/project-already-released? s "project-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :project/upsert
                                 :value {:id "project-1" :client-name "Sato Apparel"}})
        (is (= "Sato Apparel" (:client-name (store/project s "project-1"))))
        (is (= #{:logo-mark :packaging-motif} (:licensed-scope-elements (store/project s "project-1"))) "unrelated field preserved"))
      (testing "brief / risk-screen payloads commit and read back"
        (store/commit-record! s {:effect :brief/set :path ["project-1"]
                                 :payload {:jurisdiction "JPN" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "JPN" :checklist ["a" "b"]} (store/brief-of s "project-1")))
        (store/commit-record! s {:effect :risk-screen/set :path ["project-1"]
                                 :payload {:project-id "project-1" :verdict :resolved}})
        (is (= {:project-id "project-1" :verdict :resolved} (store/risk-screen-of s "project-1"))))
      (testing "deliverable release drafts a record and advances the sequence"
        (store/commit-record! s {:effect :project/mark-released :path ["project-1"]})
        (is (= "JPN-REL-000000" (get (first (store/release-history s)) "record_id")))
        (is (= "deliverable-release-draft" (get (first (store/release-history s)) "kind")))
        (is (true? (:deliverable-released? (store/project s "project-1"))))
        (is (= 1 (count (store/release-history s))))
        (is (= 1 (store/next-release-sequence s "JPN")))
        (is (true? (store/project-already-released? s "project-1")))
        (is (false? (store/project-already-released? s "project-2"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/project s "nope")))
    (is (= [] (store/all-projects s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/release-history s)))
    (is (zero? (store/next-release-sequence s "JPN")))
    (store/with-projects s {"x" {:id "x" :client-name "n"
                              :deliverable-elements #{:logo-mark}
                              :licensed-scope-elements #{:logo-mark :packaging-motif}
                              :ip-licensing-conflict-unresolved? false
                              :deliverable-released? false
                              :jurisdiction "JPN" :status :intake}})
    (is (= "n" (:client-name (store/project s "x"))))))
