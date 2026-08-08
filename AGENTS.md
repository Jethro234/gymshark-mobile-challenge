# AGENTS.md

Entry point for any agent working on this repository. **Read this file first, in full,
before reading any other document or writing any code.**

---

## 1. What this is

An Android take-home for Gymshark. It parses a supplied Algolia JSON payload and presents a
product grid and a product detail screen, with the product description rendered from HTML.

**The architecture, design and scope are already decided.** They were worked out in detail
before implementation began and are recorded in the documents below. Your job is to build
what is specified — not to redesign it, not to improve on it, and not to add to it.

If you believe a decision is wrong, **say so and stop**. Do not silently deviate. Every
significant decision already has its rejected alternatives documented, so the reasoning you
are missing is probably written down.

---

## 2. Reading order

| Order | Document | Contains |
|---|---|---|
| 1 | **`docs/SCOPE.md`** | The budget, what is cut, build order, definition of done per phase |
| 2 | **`docs/ARCHITECTURE.md`** | Modules, DI, state, data layer, navigation, testing, state restoration |
| 3 | **`docs/DESIGN.md`** | Colour and type tokens, component specs, accessibility, snapshot matrix |
| 4 | **`docs/CONVENTIONS.md`** | Git, build setup, KSP, static analysis, code style |
| 5 | `docs/PERFORMANCE.md` | Benchmark method — **read the warning at the top** |
| 6 | `README.md` | The deliverable's front page; contains placeholders |

**Precedence, highest first:**

1. `docs/SCOPE.md` — if something is cut here, it is not built, regardless of what any other
   document describes.
2. `docs/ARCHITECTURE.md` (main body — **Appendix A is a record of cuts, not a work list**)
3. `docs/DESIGN.md`
4. `docs/CONVENTIONS.md`

---

## 3. Non-negotiables

These are the graded items. Everything else can be traded away; these cannot.

1. **HTML description rendered properly.** Sanitised via a pure, unit-tested function, then
   `AnnotatedString.fromHtml`. **No WebView.** `<strong>` becomes a heading; the
   `<br>`-delimited run becomes a real bullet list.
2. **Broken and missing images handled visibly**, with a composed error state — not a grey
   box, not a static drawable.
3. **Labels rendered as indicators**, including an `Unknown(raw)` fallback for values not
   seen in the payload.
4. **Unit tests** covering mappers, `Money`, `Label`, the sanitiser, and ViewModel state
   transitions.
5. **Money is never `Double` or `Float`.** `@JvmInline value class Money(val minorUnits: Long)`.
6. **No `!!` anywhere.** No `lateinit` outside test setup.
7. **All user-facing text in `strings.xml`.** No hardcoded strings in Composables.
8. **No hardcoded colours or dimensions in `:feature:*`.** Tokens only.
9. **`start`/`end` padding, never `left`/`right`.** `AutoMirrored` icons.
10. **Every commit compiles and passes its tests.**

---

## 4. Do not invent

This is the section that matters most. Agents fail here, not on the code.

- **Do not fabricate performance numbers.** Every `TBC` in `docs/PERFORMANCE.md` stays `TBC`
  until a human runs the benchmark on a physical device. Do not estimate, extrapolate, or
  supply typical values. Build the benchmark module so the numbers *can* be produced; do
  not produce them.
- **Do not create screenshots.** README image paths are placeholders. Screenshots are
  generated from committed Paparazzi goldens, by a human, after the UI exists.
- **Do not add features.** No bottom navigation, no "Add to bag", no favourites, no search,
  no sort, no filter, no basket, no analytics, no crash reporting, no splash screen, no
  onboarding. `docs/DESIGN.md` records these as deliberate cuts. **Nothing ships that does not
  do something.**
- **Do not add dependencies** not already named in `docs/CONVENTIONS.md` or the version catalog.
  If you think one is required, stop and ask.
- **Do not invent API behaviour.** The endpoint is a static CDN file. It does not paginate,
  does not authenticate, and cannot fail. Failure handling is proved through MockWebServer,
  not by pretending the API does something it does not.
- **Do not change the design.** Tokens, spacing, radii and type scale are specified in
  `docs/DESIGN.md`. Build to the spec; do not redesign while implementing.
- **Do not write a `TODO` and move on.** Either do it or raise it.

---

## 4a. The endpoint, and the payload prerequisite

**Endpoint:**

```
https://cdn.develop.gymshark.com/training/mock-product-responses/algolia-example-payload.json
```

**Before any mapper work begins**, fetch it once and commit the response verbatim to
`core/testing/src/main/resources/algolia-example-payload.json`. Do not hand-write, trim,
reformat or "tidy" it — the Word-mangled HTML and the `null` labels are the point.

Everything downstream depends on this file: mapper tests, the `HtmlSanitiser` golden, the
deliberately-broken image fixture, and the benchmark's repeated dataset. **It is a hard
prerequisite, not a parallel task.**

The brief (*Mobile Engineering Challenge v2*, July 26) is not in the repository. Its
requirements are reproduced in `docs/ARCHITECTURE.md` §1; treat that table as the
specification.

---

## 4b. Approved dependencies

The rule in §4 is "no dependencies beyond those approved". This is the approved list. Adding
anything else requires stopping and asking.

| Purpose | Dependency |
|---|---|
| UI | Compose BOM — **`ui-text` 1.9 or later, non-negotiable** (see `docs/ARCHITECTURE.md` §6), Material 3 |
| Navigation | `androidx.navigation3`, `androidx.lifecycle:lifecycle-viewmodel-navigation3` |
| DI | Hilt + `hilt-navigation-compose`, processed via **KSP** |
| Network | Retrofit, OkHttp, `kotlinx-serialization-json`, Retrofit kotlinx-serialization converter |
| Images | Coil 3 (`coil-compose`, `coil-network-okhttp`) |
| Async | `kotlinx-coroutines`, `kotlinx-collections-immutable` |
| Unit test | JUnit 5, **`de.mannodermaus.android-junit5` Gradle plugin — approved**, Turbine, `kotlinx-coroutines-test`, MockWebServer, Truth or kotlin.test |
| Snapshot | Paparazzi |
| Instrumented | `compose-ui-test-junit4`, `hilt-android-testing` |
| Perf | `androidx.benchmark:benchmark-macro-junit4`, `androidx.baselineprofile` plugin |
| Quality | ktlint, detekt, Kover |

**On JUnit 5:** the reviewer is correct that AGP will not run JUnit 5 in an Android library
module without the Mannodermaus plugin. It is approved. `:core:model` is a pure JVM module
and needs only the standard JUnit 5 platform.

**No mocking library.** Hand-written fakes only — see `docs/ARCHITECTURE.md` §9.0.

---

## 5. Source of truth for data

`docs/ARCHITECTURE.md` §2 records what the payload actually contains, including the traps.
Before writing a mapper, read it. Summary of the things that break naive implementations:

- `labels` is `null` on nine of ten products.
- `price` is minor units — `1000` is £10.00.
- `colour` uses two different multi-value separators: `/` and ` | `.
- `fit`, `compareAtPrice`, `discountPercentage` and `alt` are all nullable. `alt` is null on
  every product.
- The ten `hits` are five distinct products in ten colourways. **Ship the list flat.**
- `description` contains a `<meta>` tag mid-body and Word clipboard classes.

The real payload is committed as a test resource. Mapper tests run against it, not against
a tidied approximation.

---

## 6. Definition of done, per unit of work

A task is complete when **all** of the following hold. Not four of five.

- [ ] Code compiles with `allWarningsAsErrors`
- [ ] ktlint, detekt and Android Lint pass with no new suppressions
- [ ] Unit tests written and passing, with assertions derived from the brief and the
      payload — **not from reading the implementation**
- [ ] Paparazzi goldens recorded where the change is visual
- [ ] No `!!`, no `lateinit`, no `TODO`, no commented-out code
- [ ] One Conventional Commit, scoped by module, subject under 72 characters, imperative
      mood, body explaining *why* if the why is not obvious

---

## 7. Commit convention

```
feat(model): add Money value class with minor-unit conversion
test(data): cover mapper against committed payload fixture
fix(designsystem): match error placeholder radius to loaded image
perf: move HTML sanitising out of composition into the mapper
```

Prefixes: `feat` · `fix` · `test` · `refactor` · `perf` · `docs` · `chore`.
Linear history, no merge commits, no `wip`. `docs/CONVENTIONS.md` §1 has the full planned
sequence — follow its ordering.

---

## 8. When to stop and ask

Stop. Do not guess.

- A specification in the documents is ambiguous or self-contradictory.
- A decision appears wrong or unbuildable as written.
- A dependency or API has changed such that the specified approach no longer works.
- The work would require adding a dependency, a feature, or a screen not specified here.
- A test cannot be made to pass without weakening its assertion.
- You are about to write a placeholder, a stub, or a mock value that could reach the
  submission.

A stopped agent with a clear question is far more useful than a finished agent that
invented its way past a problem.
