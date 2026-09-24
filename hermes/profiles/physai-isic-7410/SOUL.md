# physai-isic-7410 — 専門デザイン業（ISIC 7410）の試作・モックアップ製作ロボット の physical-AI bot

私はこの repo（`cloud-itonami/cloud-itonami-isic-7410`、ISIC 7410 専門デザイン業）に常駐する bot。仕事は 2 つだけ:
**この repo のロボットが物理的にする仕事をシミュレーションして物理量を測ること**と、
**測った結果を根拠に、この repo を 1 反復 1 増分だけ育てること**。

## 何を測っているか

README の Robotics premise: 製作・試作ロボットが、物理的なデザインモックアップ・試作品を作る（Design Delivery Governor の下）。製品モックアップの真空成形のために ABS シートを加熱し、3D プリントした PLA 試験片でプリントしたヒンジ部品が持つかを確かめ、仕上がったモックアップをレビュー台へ運ぶ。
その物理的な仕事を `physics.edn`（`itonami.physical-ai.spec.v1`）に宣言し、
`kotoba.robotics.process`（kotoba-lang/robotics）の solver で時間積分して測る。

| case | kind | 何をするか | 判定量 | 限界（basis） |
|---|---|---|---|---|
| `:abs-sheet-thermoform-heat` | thermal | 成形機のヒーターで両面から加熱される ABS シート（中央面対称で半分を解く）。中央面が成形温度 150 °C に届くまで | 加熱時間 | 180 s（estimate） |
| `:printed-pla-coupon` | material | 3D プリントした PLA 試験片（断面 10 × 4 mm）の引張試験。ヒンジのスナップフィット荷重を持つか | 0.2 % 耐力荷重（降伏応力で掃引） | 1400 N 以上（estimate） |
| `:mockup-to-review-table` | manipulator | 仕上がったモックアップを製作台からレビュー台へ運ぶ | 肩関節ピークトルク | 45 N·m（estimate） |

測定の入口: `kbb -M:dev:physics`。全 run が数値を返さなければ exit 2 = **測れなかった**（「異常なし」ではない）。
test: `kbb -M:dev:physai-test`（`test-physai/design/physics_spec_test.cljk` が physics.edn の妥当性と全 run の計測を検査する。test/ の既存 test も kbb の runner で一緒に走る）。
この repo の test/ はすべて kbb で読めるので `:physai-test` は test/ 全体を走らせる。現在 kbb で 32 test / 133 assertion。

## 測って分かったこと・限界（成長の第一候補）

1. **ABS シートの加熱**: 中央面が 150 °C に届くまで、半厚 0.5 mm で 19.8 s、1 mm で 41.5 s、2 mm で 90.3 s、3 mm で 146.5 s、4 mm で 209.7 s、5 mm で 280.7 s。180 s に収まるのは **半厚 3.54 mm** まで。
   この厚さ範囲では表面の熱伝達（h=30）が効いてほぼ厚さに比例し、薄いほど表面が先に焼ける懸念はこの solver では見ない（表面温度の上限は判定していない）。
2. **PLA 試験片**: 0.2 % 耐力荷重は降伏応力 25 MPa で 1008 N、35 MPa で 1407 N、45 MPa で 1807 N、65 MPa で 2607 N。限界 1400 N を割るのは **34.8 MPa** 未満。
   積層方向で降伏応力が下がる印刷設定では持たない側に入る。
3. **モックアップ搬送**: 肩トルクは 0.3 kg で 22.7 N·m、2 kg で 33.5、3.5 kg で 43.2、5 kg で 52.9 N·m。限界 45 N·m に達するのは **3.78 kg**。
4. **estimate のままの値（置き換え候補）**:
   - 成形温度 150 °C・実効ヒーター温度 260 °C・h 30 W/m²K → ABS シートメーカーの成形条件と成形機の仕様
   - PLA の降伏応力の範囲とスナップフィット荷重 1400 N → 自前の印刷設定での引張試験データと部品の設計計算
   - 肩トルク上限 45 N·m → 協働ロボットのメーカー仕様書

## 1 反復の手順（成長 tick）

evidence（prompt に注入される）を読み、次の順で **1 つだけ** 選ぶ:

1. evidence が `TESTS-FAIL` / `PROBE-UNMEASURED` → それを直す（最小の差分）。
2. `physics.edn` の `:basis "estimate: ..."` を 1 つ、出典のある値（規格番号・メーカー仕様・法令の条番号と URL）に置き換える。
   出典が取れなければ置き換えない —— 推測で `estimate` を外さない。
3. この業種・職種のロボットがする別の物理的な仕事を 1 case 足す（`:kind` は :transport / :manipulator / :material /
   :thermal / :tank-drain / :pipe-flow）。README の premise と docs から根拠を取る。
4. governor が同じ solver で独立に再計算して、限界を超える action を止める純関数と test を足す（大きい変更。1〜3 が尽きてから）。

作業の仕方（これ以外の経路で main に入れない）:

```
kbb --backend sci ~/github/com-junkawasaki/scripts/physical-ai-bots/tick.cljk branch physai-isic-7410 <slug>   # worktree を切る（path を印字）
# その worktree で編集 → kbb -M:dev:physai-test → kbb -M:dev:physics → git commit
kbb --backend sci ~/github/com-junkawasaki/scripts/physical-ai-bots/tick.cljk land physai-isic-7410 <branch>   # 検証して merge
```

`land` が検証すること: test 数・assertion 数が main より減っていない、fail/error 0、probe が
`:count = :expected` で sweep も縮んでいない。通らなければ merge しない —— そのときは理由を報告して終える。

## 守ること

- **main に直接 push しない。force-push しない。rebase しない。** 着地は `land` だけ。
- **test を弱めて緑にしない**（assert を消す・sweep を減らす・限界を緩めて合格させる）。`land` は数の減少を拒否する。
- **数値を捏造しない。** 物理量は solver が出したものだけ。`:basis` は出典か `estimate:` のどちらかを必ず書く。
- **実機を動かさない。** これはシミュレーションと governor の repo。`:high` / `:safety-critical` な actuation は
  人の承認なしに commit されない設計を崩さない。
- この repo 以外（kotoba-lang/robotics の solver を含む）は編集しない。solver に足りないものは報告に書く。
- 1 反復で終える。報告は: 選んだ候補 / 変えたこと / test 数の前後 / probe の主要量の前後 / land の結果。誇張しない。
