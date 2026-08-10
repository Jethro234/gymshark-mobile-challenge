# Scope and delivery plan

**Budget: 2–3 days ≈ 20 focused hours.**

The full design in `ARCHITECTURE.md` is roughly 35–50 hours of work. This document is the
cut. A complete modest submission beats an ambitious half-finished one every time — and
anything deferred is listed in the README as a conscious decision, which reads as judgement
rather than omission.

**Every Paparazzi mention below is this document doing its job — planning for it, and
naming the fallback if it fell through** (§6's "Paparazzi/JDK version friction" row is
exactly the fallback that ended up firing: its Gradle plugin turned out incompatible with
the AGP version Hilt requires, not just a JDK issue. `design.md` §8 of the
`build-product-catalogue` change has the full account; `DESIGN.md` §8 describes what was
built instead). None of it describes the shipped app.

---

## 1. What the brief actually grades

Everything else is optional, and optional things get cut first.

| Requirement | Non-negotiable |
|---|---|
| Parse the JSON, present as a product list | ✅ |
| Image per product | ✅ |
| **Handle incorrect and/or missing images** | ✅ explicitly called out |
| **Labels reflected as indicators** | ✅ explicitly called out |
| Detail screen on selection | ✅ |
| **HTML description presented appropriately** | ✅ explicitly called out |
| Compose · MVVM · **Unit testing** | ✅ stated preference |
| Clean, well-structured, testable code | ✅ stated criterion |
| Pleasant and **error free** experience | ✅ stated criterion |

---

## 2. Cut before starting

Removed from scope now, not abandoned halfway. Each is one README line under "next steps".

| Cut | Why |
|---|---|
| **Adaptive `ListDetailPaneScaffold`** | Genuinely valuable, genuinely 3–4 hours with its goldens and device checks. First thing to go on a 20-hour budget. |
| **Colourway grouping** | Sixty hits are twenty-one products in sixty colours. Shipping flat is what Algolia returns and is defensible; grouping is a new product model plus swatch state plus tests. **One README line noting it was spotted** converts an apparent oversight into visible judgement at zero cost. |
| **Robolectric UI test layer** | Paparazzi covers rendering; two instrumented tests cover navigation. The middle layer is the affordable loss. |
| **Trace-based investigation write-up** | Requires finding a real jank source to fix. Keep the Baseline Profile before/after numbers, drop the narrative. |
| **Eight modules → six** | Merge `:core:network` into `:core:data`, and `:feature:productlist` + `:feature:productdetail` into `:feature:products`. The boundary argument survives intact; four fewer build files do not need writing. |

**Final structure:** `:app` · `:core:model` · `:core:data` · `:core:designsystem` ·
`:core:testing` · `:feature:products`

---

## 3. Build order

Ordered so that if time runs out, what exists is coherent rather than half-wired.

### Day 1 — foundation and the hard part (8h)

| | Task | Est |
|---|---|---|
| **0** | **Fetch and commit the payload verbatim** to `:core:testing` resources — hard prerequisite for tasks 3, 4 and every fixture downstream. See `AGENTS.md` §4a | 0.25 |
| 1 | Project scaffold, version catalog, convention plugins, six modules. **Compose BOM must give `ui-text` 1.9+** | 2.0 |
| 2 | `Product`, `Money`, `Label` domain types + tests. Contrast test over token pairs | 1.5 |
| 3 | **`HtmlSanitiser`, written test-first** against the real description | 2.0 |
| 4 | DTOs, Retrofit, kotlinx.serialization, mapper + tests vs committed payload | 2.25 |

Doing the sanitiser on day 1 is deliberate. It is the highest-risk item — the one that can
quietly consume an afternoon — and it is one of the three things the brief explicitly calls
out. Discovering on day 3 that it's harder than expected is the failure mode.

### Day 2 — data, design system, list (8h)

| | Task | Est |
|---|---|---|
| 5 | Repository, in-memory cache, typed error mapping | 1.0 |
| 6 | MockWebServer failure suite: 500, timeout, malformed, empty | 1.0 |
| 7 | Design tokens, light + dark theme, `GsAsyncImage`, `GsLabelBadge` | 2.0 |
| 8 | `ProductListViewModel` + state tests (Turbine) | 1.5 |
| 9 | Product grid screen | 1.0 |
| 10 | Paparazzi setup + list goldens (states, label variants, image error, dark) | 1.5 |

### Day 3 — detail, wiring, evidence (6h)

| | Task | Est |
|---|---|---|
| 11 | Detail ViewModel, `SavedStateHandle`, cache-miss refetch | 1.5 |
| 12 | Detail screen incl. sanitised description rendering | 1.5 |
| 13 | Navigation 3 wiring, type-safe routes | 0.5 |
| 14 | Baseline Profile + Macrobenchmark on the real dataset (§4) | 1.0 |
| 15 | CI workflow, detekt/ktlint, README screenshots from goldens, final pass | 1.5 |

---

## 4. Benchmark dataset

**No synthetic harness needed.** The original plan assumed ten products — five rows in a
two-column grid, not enough to fling — and built a `benchmark` build variant that repeated
the committed fixtures to ~500 items so `FrameTimingMetric` would have something real to
measure. The real payload has sixty products: thirty rows, a genuine scroll. `PERFORMANCE.md`
measures the shipped dataset directly, with no separate variant, no dataset-multiplication
step, and no "measurement harness, not production behaviour" caveat to carry — the number
published is honest for the app as built. This also removes the roughly one hour task 14
would otherwise have spent wiring the variant.

Startup metrics and the Baseline Profile comparison were always honest on the real app and
are unaffected either way.

---

## 5. If day 3 runs over

Cut in this order. Each leaves a coherent submission behind.

1. **Macrobenchmark scroll** — keep the Baseline Profile itself (~20 minutes, ~30% faster
   first launch) and drop the measurement write-up.
2. **The two instrumented navigation tests** — Paparazzi still evidences every state.
3. **Paparazzi matrix down to `UiState` branches only** — drop font-scale and long-content
   goldens.
4. **CI workflow** — the tests still exist and still run locally.

**Never cut:** the HTML rendering, the image error state, label indicators, the unit test
suite, or the README. Those are the graded items.

---

## 6. Time risks

| Risk | Mitigation |
|---|---|
| `HtmlSanitiser` eats an afternoon | Scheduled day 1 while there is still room to absorb it. Timebox to 2h; if it overruns, ship a simpler sanitiser and note the limitation. |
| Convention plugins fight back | Timebox to 2h. Fallback is plain build files across six modules — uglier, not fatal. |
| Paparazzi/JDK version friction | Known rough edge. If unresolved in 45 minutes, drop snapshot testing and say so; the unit suite carries the grade. |
| Polishing the UI indefinitely | The design is already settled in `DESIGN.md`. Build to the spec, do not redesign while implementing. |

---

## 7. README "next steps" section

Deferred work, stated as choices with reasons — not a list of things that didn't get done:

- Adaptive two-pane layout for tablets and foldables (Nav 3 makes this cheap; omitted for time).
- Colourway grouping: the sixty hits are twenty-one products in sixty colours, and `handle`
  encodes it. Shipped flat because that is what the search endpoint returns; grouping would
  mean inventing a product model the API doesn't express.
- Payload persistence for offline first launch. (Coil's image disk cache is enabled — it is
  the response body, not the artwork, that an offline first launch would need.)
- Field performance monitoring — Play Vitals plus RUM attribution — which is what this
  tooling is actually for at production scale.
