(ns design.phase-test
  "The phase table as executable tests. The invariant this repo cannot
  regress on: `:actuation/release-deliverable` must NEVER be a member
  of any phase's `:auto` set."
  (:require [clojure.test :refer [deftest is testing]]
            [design.phase :as phase]))

(deftest release-deliverable-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in the future entries, auto-commits a real deliverable release"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :actuation/release-deliverable))
          (str "phase " n " must not auto-commit :actuation/release-deliverable")))))

(deftest risk-screen-never-auto-at-any-phase
  (testing "screening carries no direct capital risk, but is still never auto-eligible, matching every sibling screening op in this fleet"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :risk/screen))
          (str "phase " n " must not auto-commit :risk/screen")))))

(deftest phase-0-is-fully-read-only
  (is (empty? (:writes (get phase/phases 0)))))

(deftest phase-3-auto-commits-only-no-capital-risk-ops
  (testing ":project/intake carries no direct capital risk -- auto-eligible; it is the ONLY auto-eligible op in this domain"
    (is (= #{:project/intake} (:auto (get phase/phases 3))))))

(deftest gate-hold-always-wins
  (is (= :hold (:disposition (phase/gate 3 {:op :project/intake} :hold)))))

(deftest gate-escalates-a-clean-non-auto-write
  (is (= :escalate (:disposition (phase/gate 3 {:op :actuation/release-deliverable} :commit)))))

(deftest gate-holds-a-write-disabled-in-this-phase
  (is (= :hold (:disposition (phase/gate 0 {:op :project/intake} :commit)))))
