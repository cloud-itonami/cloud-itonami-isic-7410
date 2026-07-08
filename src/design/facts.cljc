(ns design.facts
  "Per-jurisdiction design-industry professional-standards/
  intellectual-property regulatory catalog -- the G2-style spec-basis
  table the Design Delivery Governor checks every `:brief/verify`
  proposal against ('did the advisor cite an OFFICIAL public source
  for this jurisdiction's design-professional-standards and IP-
  clearance framework, or did it invent one?').

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries.

  Seed values are drawn from each jurisdiction's official IP/design-
  registration authority (see `:provenance`); they are a STARTING
  catalog, not a from-scratch survey of all ~194 jurisdictions.
  Extending coverage is additive: add one map to `catalog`, cite a
  real source, done -- never invent a jurisdiction's requirements to
  make coverage look bigger.")

(def catalog
  "iso3 -> requirement map. `:required-evidence` mirrors the generic
  design-brief-record/concept-approval-record/licensing-clearance-
  record/ip-assignment-record evidence set every prior sibling's
  evidence checklist submits in some form; `:legal-basis` /
  `:owner-authority` / `:provenance` are the G2 citation the governor
  requires before any `:actuation/release-deliverable` proposal can
  commit."
  {"JPN" {:name "Japan"
          :owner-authority "特許庁 (Japan Patent Office, JPO) -- 意匠制度"
          :legal-basis "意匠法 (Design Act) / 著作権法 (Copyright Act)"
          :national-spec "特定デザイン業務における意匠・著作権クリアランスおよび成果物引渡し要件"
          :provenance "https://www.jpo.go.jp/system/design/"
          :required-evidence ["デザインブリーフ記録 (design-brief-record)"
                              "コンセプト承認記録 (concept-approval-record)"
                              "ライセンスクリアランス記録 (licensing-clearance-record)"
                              "知的財産権譲渡記録 (ip-assignment-record)"]}
   "USA" {:name "United States"
          :owner-authority "U.S. Copyright Office / U.S. Patent and Trademark Office (USPTO)"
          :legal-basis "Copyright Act of 1976, 17 U.S.C. / design-patent provisions, 35 U.S.C. § 171"
          :national-spec "Design-studio IP-clearance and final-deliverable-release requirements"
          :provenance "https://www.copyright.gov/help/faq/"
          :required-evidence ["Design-brief record"
                              "Concept-approval record"
                              "Licensing-clearance record"
                              "IP-assignment record"]}
   "GBR" {:name "United Kingdom"
          :owner-authority "UK Intellectual Property Office (UK IPO)"
          :legal-basis "Copyright, Designs and Patents Act 1988"
          :national-spec "Regulated design-studio IP-clearance and deliverable-release requirements"
          :provenance "https://www.gov.uk/government/organisations/intellectual-property-office"
          :required-evidence ["Design-brief record"
                              "Concept-approval record"
                              "Licensing-clearance record"
                              "IP-assignment record"]}
   "DEU" {:name "Germany"
          :owner-authority "Deutsches Patent- und Markenamt (DPMA)"
          :legal-basis "Gesetz über den rechtlichen Schutz von Design (DesignG) / Urheberrechtsgesetz (UrhG)"
          :national-spec "Anforderungen an Designstudios zur Rechteklärung und Lieferungsfreigabe"
          :provenance "https://www.dpma.de/designs/"
          :required-evidence ["Designbriefingprotokoll (design-brief-record)"
                              "Konzeptfreigabeprotokoll (concept-approval-record)"
                              "Lizenzklärungsprotokoll (licensing-clearance-record)"
                              "Rechteübertragungsprotokoll (ip-assignment-record)"]}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to release a
  deliverable on it."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-7410 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `design.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  "Does `submitted` (a set/coll of evidence keywords or strings) satisfy
  every evidence item listed for `iso3`? Missing spec-basis -> never
  satisfied."
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))
