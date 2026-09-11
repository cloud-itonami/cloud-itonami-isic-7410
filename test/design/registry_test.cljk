(ns design.registry-test
  (:require [clojure.test :refer [deftest is]]
            [design.registry :as r]))

;; ----------------------------- deliverable-scope-exceeded? -----------------------------

(deftest not-exceeded-when-a-subset-of-licensed-scope
  (is (not (r/deliverable-scope-exceeded? {:deliverable-elements #{:logo-mark}
                                           :licensed-scope-elements #{:logo-mark :packaging-motif}})))
  (is (not (r/deliverable-scope-exceeded? {:deliverable-elements #{:logo-mark :packaging-motif}
                                           :licensed-scope-elements #{:logo-mark :packaging-motif}})))
  (is (not (r/deliverable-scope-exceeded? {:deliverable-elements #{}
                                           :licensed-scope-elements #{:logo-mark}}))))

(deftest exceeded-when-includes-an-element-outside-licensed-scope
  (is (r/deliverable-scope-exceeded? {:deliverable-elements #{:logo-mark :stock-photo-set}
                                      :licensed-scope-elements #{:logo-mark :packaging-motif}}))
  (is (r/deliverable-scope-exceeded? {:deliverable-elements #{:stock-photo-set}
                                      :licensed-scope-elements #{:logo-mark :packaging-motif}})))

(deftest exceeded-is-false-on-missing-fields
  (is (not (r/deliverable-scope-exceeded? {})))
  (is (not (r/deliverable-scope-exceeded? {:deliverable-elements #{:stock-photo-set}}))))

;; ----------------------------- register-deliverable-release -----------------------------

(deftest release-is-a-draft-not-a-real-release
  (let [result (r/register-deliverable-release "project-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest release-assigns-release-number
  (let [result (r/register-deliverable-release "project-1" "JPN" 7)]
    (is (= (get result "release_number") "JPN-REL-000007"))
    (is (= (get-in result ["record" "project_id"]) "project-1"))
    (is (= (get-in result ["record" "kind"]) "deliverable-release-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest release-validation-rules
  (is (thrown? Exception (r/register-deliverable-release "" "JPN" 0)))
  (is (thrown? Exception (r/register-deliverable-release "project-1" "" 0)))
  (is (thrown? Exception (r/register-deliverable-release "project-1" "JPN" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-deliverable-release "project-1" "JPN" 0)
        hist (r/append [] c1)
        c2 (r/register-deliverable-release "project-2" "JPN" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "JPN-REL-000000" (get-in hist2 [0 "record_id"])))
    (is (= "JPN-REL-000001" (get-in hist2 [1 "record_id"])))))
