(ns design.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean project through
  intake -> brief verification -> IP/licensing-conflict screening ->
  deliverable-release proposal (always escalates) -> human approval ->
  commit, then shows four HARD holds (a jurisdiction with no spec-
  basis, a deliverable whose own elements exceed its own recorded
  licensed scope, an unresolved IP/licensing conflict screened
  directly via `:risk/screen` [never via an actuation op against an
  unscreened project -- see this actor's own governor ns docstring /
  the lesson `parksafety`'s ADR-2607071922 Decision 5, `eldercare`'s,
  `museum`'s, `conservation`'s, `salon`'s, `entertainment`'s,
  `casework`'s, `hospital`'s, `facility`'s, `school`'s, `association`'s,
  `leasing`'s, `behavioral`'s, `secondary`'s, `card`'s, `water`'s,
  `telecom`'s, `aerospace`'s, `recovery`'s, `consulting`'s, `union`'s,
  `congregation`'s, `fab`'s, `energy`'s, `care`'s, `navigator`'s,
  `learning`'s, `banking`'s, `advertising`'s, `polling`'s and
  `research`'s ADR-0001s already recorded], and a double release of an
  already-processed project) that never reach a human at all, and
  prints the audit ledger + the draft deliverable-release records."
  (:require [langgraph.graph :as g]
            [design.store :as store]
            [design.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :studio-operator :phase 3})

(defn- exec! [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== project/intake project-1 (JPN, clean; deliverable within licensed scope, no IP/licensing conflict) ==")
    (println (exec! actor "t1" {:op :project/intake :subject "project-1"
                                :patch {:id "project-1" :client-name "Sato Apparel"}} operator))

    (println "== brief/verify project-1 (escalates -- human approves) ==")
    (println (exec! actor "t2" {:op :brief/verify :subject "project-1"} operator))
    (println (approve! actor "t2"))

    (println "== risk/screen project-1 (clean; escalates -- human approves) ==")
    (println (exec! actor "t3" {:op :risk/screen :subject "project-1"} operator))
    (println (approve! actor "t3"))

    (println "== actuation/release-deliverable project-1 (always escalates -- actuation/release-deliverable) ==")
    (let [r (exec! actor "t4" {:op :actuation/release-deliverable :subject "project-1"} operator)]
      (println r)
      (println "-- human studio-operator approves --")
      (println (approve! actor "t4")))

    (println "== brief/verify project-2 (no spec-basis -> HARD hold) ==")
    (println (exec! actor "t5" {:op :brief/verify :subject "project-2" :no-spec? true} operator))

    (println "== brief/verify project-3 (escalates -- human approves; sets up the deliverable-scope-exceeded test) ==")
    (println (exec! actor "t6" {:op :brief/verify :subject "project-3"} operator))
    (println (approve! actor "t6"))

    (println "== actuation/release-deliverable project-3 (deliverable elements exceed licensed scope -> HARD hold) ==")
    (println (exec! actor "t7" {:op :actuation/release-deliverable :subject "project-3"} operator))

    (println "== risk/screen project-4 (unresolved -> HARD hold, never reaches a human) ==")
    (println (exec! actor "t8" {:op :risk/screen :subject "project-4"} operator))

    (println "== actuation/release-deliverable project-1 AGAIN (double-release -> HARD hold) ==")
    (println (exec! actor "t9" {:op :actuation/release-deliverable :subject "project-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft deliverable-release records ==")
    (doseq [r (store/release-history db)] (println r))))
