(ns design.designadvisor
  "Designer-LLM client -- the *contained intelligence node* for the
  design actor (README: \"Designer-LLM\").

  It normalizes project-intake, drafts a per-jurisdiction design-
  professional-standards evidence checklist, screens projects for an
  unresolved IP/licensing conflict, and drafts the deliverable-release
  action. CRITICAL: it is a smart-but-untrusted advisor. It returns a
  *proposal* (with a rationale + the fields it cited), never a
  committed record or a real deliverable release. Every output is
  censored downstream by `design.governor` before anything touches
  the SSoT, and `:actuation/release-deliverable` proposals NEVER
  auto-commit at any phase -- see README `Actuation`.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the spec-basis gate
     :cites      [kw|str ..]    ; facts/sources the LLM used -- SCANNED too
     :effect     kw             ; how a commit would mutate the SSoT
     :stake      kw|nil         ; :actuation/release-deliverable | nil
     :confidence 0..1}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [clojure.string :as str]
            [design.facts :as facts]
            [design.registry :as registry]
            [design.store :as store]
            [langchain.model :as model]))

(defn- normalize-intake
  "Directory upsert -- the LLM only normalizes/validates the patch; it
  does not invent the project, jurisdiction or licensed scope. High
  confidence, low stakes."
  [_db {:keys [patch]}]
  {:summary    (str "案件記録更新: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :project/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- verify-brief
  "Per-jurisdiction design-professional-standards evidence checklist
  draft. `:no-spec?` injects the failure mode we must defend against:
  proposing a checklist for a jurisdiction with NO official spec-basis
  in `design.facts` -- the Design Delivery Governor must reject this
  (never invent a jurisdiction's requirements)."
  [db {:keys [subject no-spec?]}]
  (let [p (store/project db subject)
        iso3 (if no-spec? "ATL" (:jurisdiction p))
        sb (facts/spec-basis iso3)]
    (if (nil? sb)
      {:summary    (str iso3 " の公式spec-basisが見つかりません")
       :rationale  "design.facts に未登録の法域。要件を推測で作らない。"
       :cites      []
       :effect     :brief/set
       :value      {:jurisdiction iso3 :checklist [] :spec-basis nil}
       :stake      nil
       :confidence 0.9}
      {:summary    (str iso3 " (" (:owner-authority sb) ") 向け必要書類 "
                        (count (:required-evidence sb)) " 件を提案")
       :rationale  (str "公式ソース: " (:provenance sb) " / 法的根拠: " (:legal-basis sb))
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :brief/set
       :value      {:jurisdiction iso3
                    :checklist (:required-evidence sb)
                    :spec-basis (:provenance sb)
                    :legal-basis (:legal-basis sb)}
       :stake      nil
       :confidence 0.9})))

(defn- screen-ip-licensing-conflict
  "IP/licensing-conflict screening draft. `:ip-licensing-conflict-
  unresolved?` on the project record injects the failure mode: the
  Design Delivery Governor must HOLD, un-overridably, on any
  unresolved conflict."
  [db {:keys [subject]}]
  (let [p (store/project db subject)]
    (cond
      (nil? p)
      {:summary "対象案件記録が見つかりません" :rationale "no project record"
       :cites [] :effect :risk-screen/set :value {:project-id subject :verdict :unknown}
       :stake nil :confidence 0.0}

      (true? (:ip-licensing-conflict-unresolved? p))
      {:summary    (str (:client-name p) ": 未解決の知的財産権/ライセンス紛争を検出")
       :rationale  "スクリーニングが未解決の知的財産権/ライセンス紛争を検出。人手確認とホールドが必須。"
       :cites      [:ip-licensing-check]
       :effect     :risk-screen/set
       :value      {:project-id subject :verdict :unresolved}
       :stake      nil
       :confidence 0.95}

      :else
      {:summary    (str (:client-name p) ": 未解決の知的財産権/ライセンス紛争なし")
       :rationale  "知的財産権/ライセンス紛争スクリーニング完了。"
       :cites      [:ip-licensing-check]
       :effect     :risk-screen/set
       :value      {:project-id subject :verdict :resolved}
       :stake      nil
       :confidence 0.9})))

(defn- propose-deliverable-release
  "Draft the actual DELIVERABLE-RELEASE action -- releasing a real
  final deliverable to a client. ALWAYS `:stake :actuation/release-
  deliverable` -- this is a REAL-WORLD design act, never a draft the
  actor may auto-run. See README `Actuation`: no phase ever adds this
  op to a phase's `:auto` set (`design.phase`); the governor also
  always escalates on `:actuation/release-deliverable`. Two
  independent layers agree, deliberately."
  [db {:keys [subject]}]
  (let [p (store/project db subject)]
    {:summary    (str subject " 向け成果物引渡し提案"
                      (when p (str " (client=" (:client-name p) ")")))
     :rationale  (if p
                   (str "deliverable-elements=" (:deliverable-elements p)
                        " licensed-scope-elements=" (:licensed-scope-elements p))
                   "案件記録が見つかりません")
     :cites      (if p [subject] [])
     :effect     :project/mark-released
     :value      {:project-id subject}
     :stake      :actuation/release-deliverable
     :confidence (if (and p (not (registry/deliverable-scope-exceeded? p))) 0.9 0.3)}))

(defn infer
  "Route a request to the right proposal generator.
  request: {:op kw :subject id ...op-specific...}"
  [db {:keys [op] :as request}]
  (case op
    :project/intake                     (normalize-intake db request)
    :brief/verify                       (verify-brief db request)
    :risk/screen                        (screen-ip-licensing-conflict db request)
    :actuation/release-deliverable       (propose-deliverable-release db request)
    {:summary "未対応の操作" :rationale (str op) :cites []
     :effect :noop :stake nil :confidence 0.0}))

;; ----------------------------- Advisor protocol -----------------------------

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

(defn mock-advisor
  "The deterministic advisor (the `infer` logic above). Default everywhere."
  [] (reify Advisor (-advise [_ st req] (infer st req))))

(def ^:private system-prompt
  (str "あなたはデザインスタジオの成果物引渡しエージェントの助言者です。"
       "与えられた事実のみに基づき、提案を1つだけEDNマップで返します。説明や前置きは"
       "一切書かず、EDNだけを出力します。\n"
       "キー: :summary(人向けドラフト) :rationale(根拠/必ず事実から) "
       ":cites(使った事実キーのベクタ) "
       ":effect(:project/upsert|:brief/set|:risk-screen/set|"
       ":project/mark-released) "
       ":stake(:actuation/release-deliverable か nil) :confidence(0..1)。\n"
       "重要: 登録されていない法域の要件を絶対に創作してはいけません。"
       "spec-basisが無い場合は :cites を空にし confidence を上げないこと。"))

(defn- facts-for [st {:keys [op subject]}]
  (case op
    :brief/verify                       {:project (store/project st subject)}
    :risk/screen                        {:project (store/project st subject)}
    :actuation/release-deliverable       {:project (store/project st subject)}
    {:project (store/project st subject)}))

(defn- parse-proposal
  "Parse the model's EDN proposal defensively. Any parse/shape failure
  yields a safe low-confidence noop so the Design Delivery Governor
  escalates/holds -- an LLM hiccup can never auto-release a
  deliverable."
  [content]
  (let [p (try (edn/read-string (str/trim (str content)))
               (catch #?(:clj Exception :cljs :default) _ nil))]
    (if (map? p)
      (-> p
          (update :cites #(vec (or % [])))
          (update :confidence #(if (number? %) (double %) 0.0))
          (update :effect #(or % :noop)))
      {:summary "LLM応答を解釈できませんでした" :rationale (str content)
       :cites [] :effect :noop :stake nil :confidence 0.0})))

(defn llm-advisor
  "An advisor backed by a `langchain.model/ChatModel` (real inference)."
  ([chat-model] (llm-advisor chat-model {}))
  ([chat-model gen-opts]
   (reify Advisor
     (-advise [_ st req]
       (let [msgs [{:role :system :content system-prompt}
                   {:role :user :content (str "操作: " (:op req)
                                              "\n対象: " (:subject req)
                                              "\n事実: " (pr-str (facts-for st req)))}]
             resp (model/-generate chat-model msgs gen-opts)]
         (parse-proposal (:content resp)))))))

(defn trace
  "Decision-grounded audit record -- persisted to the :audit channel."
  [request proposal]
  {:t          :designadvisor-proposal
   :op         (:op request)
   :subject    (:subject request)
   :summary    (:summary proposal)
   :rationale  (:rationale proposal)
   :cites      (:cites proposal)
   :confidence (:confidence proposal)})
