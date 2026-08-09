# Gymshark Mobile Engineering Challenge — Architecture Decision Record

**Author:** James Buxton
**Date:** August 2026
**Status:** Agreed, pre-implementation

> **This document describes exactly what is to be built.** Work that was designed and then
> cut for budget is in Appendix A and must not be implemented. Read `../AGENTS.md` first.

This document records the decisions taken before a line of code was written, and — more
importantly — the reasoning and the alternatives rejected. Where the brief or the payload
was ambiguous, the assumption is stated explicitly rather than hidden in an implementation.

---

## 1. What the brief actually asks for

From *Mobile Engineering Challenge v2*:

| Requirement | Where it is satisfied |
|---|---|
| Parse JSON from the endpoint, present as a product list | `:core:data` → `:feature:products` |
| Show an image for each product | `GsAsyncImage` in `:core:designsystem` |
| Handle incorrect and/or missing images | Explicit error/loading states + deliberately broken fixture |
| Reflect product "Labels" as indicators | `Label` sealed interface + `GsLabelBadge` |
| Selecting a product shows further information | `:feature:products` |
| HTML description presented appropriately | `HtmlSanitiser` + `AnnotatedString.fromHtml` |
| Clean, well-structured, **testable** code | Module boundaries, pure-Kotlin domain, unit + snapshot + instrumented |
| Pleasant and **error free** UX | Sealed `UiState`, typed errors, retry, pull-to-refresh |
| Compose / MVVM / unit testing preference | Compose + MVVM + JUnit5 |

---

## 2. Findings from the payload

The endpoint returns an **Algolia search response** whose only top-level key is `hits` — no
`nbHits`, `page` or `hitsPerPage`, despite the "Algolia search response" framing — containing
**60 products**. Reading it carefully changes several design decisions:

1. **`price` is `1000` on 54 of 60 products; the other six are `50`, `60` (×4) and `65`.**
   Interpreted as *major* units (pounds) — see §7. The `1000` products render as £1,000.00,
   as supplied, not treated as a placeholder.
2. **`labels` is `null` on 51 products, an explicit empty array on one, and populated on
   eight.** Six distinct values appear, splitting into two categories — merchandising
   (`going-fast` ×4, `new`, `limited-edition`, `popular`) and sustainability
   (`recycled-nylon`, `recycled-polyester`). One product carries all three of `new`,
   `recycled-nylon` and `recycled-polyester` at once. See §8.
3. **`description` is Shopify/TinyMCE HTML pasted out of Microsoft Word.** Contains a
   `<meta charset="utf-8">` *inside the body string*, `data-mce-fragment` attributes
   throughout, dead Word classes (`class="TextRun SCXP103297068 BCX0"`), and bullet lists
   built from `<br>` rather than `<ul>`. See §6.
4. **`colour` carries multiple values with two different separators:**
   `"Court Blue/Moonstone Blue/White"` and `"Savanna | Cherry Brown"`.
5. **Nullable fields:** `fit`, `compareAtPrice`, `discountPercentage`, `alt`, `labels`.
   `compareAtPrice` and `discountPercentage` are `null` on **every** product in this
   payload, not just most of them.
6. **`featuredMedia` plus a `media` array** of 5–7 images per product, each with
   `width`/`height` (usable for aspect ratio before load — no layout jump).
7. **Sixty hits are twenty-one distinct products in sixty colourways.** `Adapt Camo
   Seamless Leggings` ×6, `WTFlex Seamless High Waisted Leggings` ×6, `Flex High Waisted
   Leggings` ×5, and so on down to single-colourway products — `handle` encodes the
   colourway (`gymshark-speed-leggings-navy-ss22`). The list ships **flat**, as the search
   endpoint returns it; grouping into one card with colour swatches would mean inventing a
   product model the API does not express. Noted in the README so it reads as a decision
   rather than an oversight.
8. **It is a static CDN file.** It cannot 500, cannot time out under normal conditions,
   cannot return an empty result, and has no page tokens. Every failure mode the brief
   asks us to handle is therefore only demonstrable through deliberate test seams. See §9.

---

## 3. Module structure

```
:app                  Hilt root, NavDisplay, theme wiring. No business logic.
:core:model           Pure Kotlin/JVM. Product, Label, Money, HtmlSanitiser. Zero Android deps.
:core:data            Retrofit + kotlinx.serialization, DTOs, mappers, ProductRepository, cache.
:core:designsystem    Theme tokens, GsAsyncImage, GsLabelBadge, previews.
:core:testing         Shared fixtures, MainDispatcherRule, Paparazzi base, fake repositories.
:feature:products     ProductList and ProductDetail — ViewModels, screens, UiState.
```

**Six modules, not eight.** `:core:network` is merged into `:core:data`, and the two feature
screens share one `:feature:products` module. The boundary argument is unaffected — features
still cannot reach each other, `:core:model` is still pure Kotlin — and four build files do
not need writing. See `SCOPE.md` for the budget this serves.

**Rules that make this structure real rather than decorative:**

- **Features never depend on other features.** Navigation between them is resolved in
  `:app`. This is the constraint that proves the pattern is understood rather than copied.
- **`:core:model` has no Android dependency at all.** Its tests are plain JVM tests and run
  in milliseconds. Mapper, `Money` and `HtmlSanitiser` tests all live here.
- **`:core:testing` exists.** Fixtures defined once, used by unit tests, Paparazzi tests and
  instrumented tests. This is the module most take-homes omit.

**Rejected:** single-module with strict packages (faster, and arguably right for two
screens, but forfeits any demonstration of boundary design); layer-based modularisation
(`:domain` / `:data` / `:ui`) — correct for small apps but doesn't scale the way a real
retail codebase does, where teams own features, not layers.

**Acknowledged cost:** for a two-screen app this is more Gradle than strictly necessary.
The trade is deliberate: the brief is a demonstration of structural judgement, and the
convention-plugin setup is itself part of the answer.

---

## 4. Dependency injection — Hilt

Chosen for three reasons, in order:

1. Compile-time verification of the graph.
2. `@TestInstallIn` / `@BindValue` make swapping a fake repository into an instrumented
   test a two-line change — testability is the brief's stated criterion.
3. It is the default at the scale Gymshark operates. A reviewer sees what they expect and
   spends their attention on the interesting parts of the code instead.

`@HiltViewModel` for both ViewModels. Scoping: `@Singleton` for `OkHttpClient`, the
`Retrofit` instance and `ProductRepository` (the cache must outlive the list screen so the
detail screen can read from it — see §10).

**Rejected:** Koin (faster builds, runtime rather than compile-time safety — defensible but
requires an argument where Hilt requires none); manual DI (cleanest for this size, but in a
take-home reads as avoiding the framework rather than transcending it).

---

## 5. Presentation — MVVM with a sealed UiState

```kotlin
sealed interface ProductListUiState {
    data object Loading : ProductListUiState
    data class Content(
        val products: List<ProductUiModel>,
        val isRefreshing: Boolean = false,
    ) : ProductListUiState
    data object Empty : ProductListUiState
    data class Error(val cause: ErrorCause) : ProductListUiState
}

sealed interface ErrorCause {
    data object NoConnection : ErrorCause   // "You're offline"
    data object Server : ErrorCause         // "We couldn't reach the store"
    data object Malformed : ErrorCause      // "Something went wrong"
    data object Unknown : ErrorCause
}
```

Why this shape:

- **Illegal states are unrepresentable.** No object can be simultaneously loading, errored
  and populated. A flags-based `data class` permits that nonsense and is trivially
  criticised.
- **`when` is exhaustive**, so adding a state is a compile error at every call site rather
  than a silent blank screen.
- **Each branch is independently snapshot-testable** — one Paparazzi golden per state.
- **`ErrorCause` is typed**, so the UI can distinguish "you're offline" from "we broke",
  which is the difference between an error state and an apology. `Throwable` in state is
  a leak of the data layer into the UI.

State is exposed as `StateFlow` via `stateIn(viewModelScope, WhileSubscribed(5_000), Loading)`
— the 5-second stop timeout is what keeps a rotation from re-triggering the network call.

`isRefreshing` lives inside `Content` rather than as a sibling state, because a
pull-to-refresh must not blank the list the user is already looking at.

---

## 6. The HTML description

**Decision: sanitise → `AnnotatedString.fromHtml` → styled Compose `Text`.**

### Why the payload looks the way it does

It is not synthetic. It is a genuine Shopify Admin API dump: merchandising copy authored in
TinyMCE (`data-mce-fragment`) into which someone pasted from Microsoft Word (the stray
`<meta charset="utf-8">` and `class="TextRun SCXP103297068 BCX0"` are Word's clipboard
signature). Nobody cleaned it, because on the web it renders acceptably.

### What the exercise is testing, in priority order

1. **Do you render HTML as HTML?** The brief says the description "should be presented to
   the user appropriately". Shipping `Text(product.description)` puts `<p data-mce-fragment="1">`
   on screen. This is the pass/fail gate.
2. **Do you reach for a WebView?** It works, and it is wrong: per-screen memory cost, no
   theming, ignores the user's font scale, misbehaves inside a scrolling column, and is
   effectively untestable.
3. **Do you treat upstream content as hostile?** Retail CMS content is always this messy.
   The sanitiser is therefore a **pure function in `:core:model`**, so `sanitise(garbage)`
   → expected output is a permanent regression test.
4. **Do you preserve authorial intent?** `<strong>RUN WITH IT</strong>` is a heading. The
   `- Full length legging<br>- High-waisted<br>` run is a bullet list wearing a `<br>`
   costume. Flattening it to one paragraph is "handled" but not *pleasant*, and the brief
   asks for both.

### Implementation

```kotlin
fun interface HtmlSanitiser {
    /** Returns the heading line (from a leading <strong> paragraph) and the remaining HTML. */
    fun sanitise(raw: String): SanitisedDescription
}

data class SanitisedDescription(val heading: String?, val bodyHtml: String)
```

Ordered, individually tested steps: strip `<meta>` (every occurrence — the real payload has a
second one embedded mid-description, inside a bullet item, not just the leading one); strip a
stray `<div>` wrapper (one description carries a dead Google Translate widget,
`<div id="gtx-trans">`); strip `data-mce-*` and Word `class` attributes; collapse `<span>`
wrappers that carry no semantics; **extract a leading `<strong>`-only paragraph as
`heading`**; convert `<br>`-delimited `- ` runs into `<ul><li>`; collapse whitespace and
empty paragraphs, including insignificant newlines the source formats between blocks.

### Version floor — load-bearing

**Compose `ui-text` 1.9 or later is required.** Earlier versions of
`AnnotatedString.fromHtml` had no bullet support: Android's parser produces a `BulletSpan`
for `<ul><li>`, and there was no `AnnotatedString` equivalent, so lists rendered as plain
lines.

`ui-text` 1.9 added `Bullet` (an `AnnotatedString.Annotation`), `BulletScope`,
`withBulletList` / `withBulletListItem`, and `fromHtml` now emits them for `<ul>`/`<li>` —
including nested lists, provided a nested `<ul>` sits *inside* an `<li>`. `Bullet` carries
`DefaultIndentation = 1.em`, `DefaultSize = 0.25.em`, `DefaultPadding = 0.25.em`.

**Pin the Compose BOM accordingly and verify with a Paparazzi golden before building the
detail screen.** If the bullets do not render, the version floor is wrong — do not work
around it by emitting bullet glyphs as text.

`fromHtml` still *silently ignores* tags it does not understand, so sanitising remains
correctness rather than politeness.

### Where the conversion runs

`sanitise` is pure and JVM-testable. `AnnotatedString.fromHtml` is **not** — it is backed by
`android.text.Html` and needs an Android runtime, which a JUnit 5 JVM test does not have.

Therefore:

- The UI model carries `heading: String?` and `bodyHtml: String` — **plain strings**.
  `ProductDetailViewModel` tests stay pure JUnit 5.
- The composable does `remember(bodyHtml) { AnnotatedString.fromHtml(bodyHtml) }`, so the
  conversion runs **once per description**, not per frame.
- `heading` renders with the `eyebrow` token — not via `<h1>`–`<h6>`, whose Android
  `RelativeSizeSpan` multipliers would override the type scale in `DESIGN.md`.

The performance claim is therefore "nothing is computed **per frame**", not "nothing during
composition". The expensive, hostile, regression-prone half stays pure and JVM-tested.

**Rejected:** a full block-model parser producing `sealed interface DescriptionBlock`
(maximum control and testability, but more machinery than this content justifies — the
sanitiser leaves the seam open if it were ever needed); `AndroidView` + `HtmlCompat`
(least code, Compose-hostile island, awkward to snapshot).

---

## 7. Money

**Assumption: `price` and `compareAtPrice` are in major units (pounds), not minor units.**
`1000` → £1,000.00, `50` → £50.00, `65` → £65.00.

This reverses the original minor-units assumption, which the real payload disproves. The
distinct, non-`1000` values in the real data are `50`, `60` and `65`. Under a minor-units
reading those render as £0.50/£0.60/£0.65 — implausible for leggings. Under a major-units
reading they render as £50/£60/£65 — Gymshark's actual legging price range. `1000` then
reads as an unset default on the other 54 products, not a genuine price point, but it is
**rendered as supplied — £1,000.00 — never special-cased**. Treating a specific numeric
value as "the placeholder" would be inventing API behaviour the payload does not state; the
honest response to an ambiguous default is to display it like any other value and note the
oddity in the README, not to guess at intent.

```kotlin
@JvmInline
value class Money(val minorUnits: Long) {
    companion object {
        const val MINOR_UNITS_PER_MAJOR = 100L
        fun fromMajorUnits(major: Long): Money = Money(major * MINOR_UNITS_PER_MAJOR)
    }
}
```

- **Never `Double` or `Float`.** Binary floating point cannot represent 0.1; currency
  arithmetic in `Double` is a defect waiting for a discount calculation.
- **The internal representation is unchanged** — `Money` still stores minor units as a
  `Long`. Only the mapper changes: it calls `Money.fromMajorUnits(price)` instead of
  `Money(price)`. `MINOR_UNITS_PER_MAJOR` is still the single named constant, so if this
  reading is wrong too, it is a one-line reversal back to treating the payload value as
  already-minor-units, and both interpretations are covered by unit tests.
- Formatting via `NumberFormat.getCurrencyInstance(locale)` — no hardcoded `£`.
- `compareAtPrice` non-null and greater than `price` drives the strikethrough and the
  discount badge; `discountPercentage` is displayed only when present, never recomputed
  (recomputing risks disagreeing with the merchandiser's own figure). Both fields are
  `null` on every product in the committed payload, so this logic is real, implemented and
  unit-tested against constructed fixtures, but not observable by running the app against
  the live payload — the same schema-driven-nullable-field treatment already given to
  `fit` (§15).

---

## 8. Labels

The real payload's six label values split cleanly into two categories that retail already
treats differently — urgency/novelty merchandising versus material provenance — and that
split resolves how a product with labels from both categories is displayed (see
`DESIGN.md` §4): merchandising labels render as the existing image badge; sustainability
labels render as chips near the description on the detail screen. No stacking or overflow
affordance is needed because the two never compete for the same slot.

```kotlin
sealed interface Label {
    val raw: String
    val category: LabelCategory

    // Merchandising — urgency and novelty, shown as the image badge.
    data object GoingFast      : Label { override val raw = "going-fast"; override val category = LabelCategory.Merchandising }
    data object New            : Label { override val raw = "new"; override val category = LabelCategory.Merchandising }
    data object LimitedEdition : Label { override val raw = "limited-edition"; override val category = LabelCategory.Merchandising }
    data object Popular        : Label { override val raw = "popular"; override val category = LabelCategory.Merchandising }

    // Sustainability — material provenance, shown as a detail-screen chip.
    data object RecycledNylon     : Label { override val raw = "recycled-nylon"; override val category = LabelCategory.Sustainability }
    data object RecycledPolyester : Label { override val raw = "recycled-polyester"; override val category = LabelCategory.Sustainability }

    // Unrecognised values default to Merchandising: the badge already has a
    // quiet, generic treatment for Unknown; a chip does not.
    data class Unknown(override val raw: String) : Label { override val category = LabelCategory.Merchandising }
}

enum class LabelCategory { Merchandising, Sustainability }
```

- **`Unknown(raw)` is the point.** A merchandiser adding a label in Shopify next week must
  not crash the app or produce a blank badge. Unknown labels render as a neutral badge with
  the raw value title-cased, so new data degrades gracefully rather than disappearing.
- Parsing is null-safe, case-insensitive and trims. An explicit empty array (`labels: []`,
  present once in the real payload) and a `null` `labels` field are semantically identical
  — both mean "no labels" — and are tested as such, since the empty-array case is a real
  value in the data, not a hypothetical.
- **At most one merchandising label is shown per product.** Every product in the payload
  carries at most one; if a product ever carried two, the first by array order would be
  shown rather than stacking a second badge — an inferred rule for a case the payload does
  not exercise, covered by a constructed fixture the same way `Unknown` needed one before
  the label vocabulary was known to be broader than `going-fast`.
- Sustainability labels are additive, not exclusive: a product can show zero, one or both
  material chips alongside at most one merchandising badge.

**Rejected:** modelling only the four merchandising values and treating sustainability
labels as `Unknown` (loses genuine material information the badge's quiet treatment isn't
designed to carry); a single flat list with no category distinction (forces a stacking or
overflow rule for the one product that actually needs both a badge and material
information, which is worse UX for no benefit); raw strings only (never drops data, but no
per-label styling and the domain layer learns nothing).

---

## 9. Testing strategy

Three layers. Depth over breadth — a small number of tests that assert something real.

### 9.0 Method, doubles and fixtures

**Not TDD.** The requirements are fixed and fully known before a line is written; test-first
would be ceremony. Tests are written alongside each unit as it is completed.

**One deliberate exception: `HtmlSanitiser` is written test-first.** It is a pure function
with a known-hostile input already in hand and a precisely definable output — the one place
where writing the expected clean string first is genuinely faster than sanitising by eye and
retrofitting an assertion. A tool, not a methodology.

**Guarding against the test-after failure mode.** Tests written after the code tend to
assert what the code *does* rather than what it *should* do. Two rules: every assertion is
derived from the brief and the payload, never from reading the function body; and for any
non-obvious test, the implementation is deliberately broken once to confirm the test fails.

**Hand-written fakes. No mocking library.** `FakeProductRepository` in `:core:testing`, with
settable responses and error triggers. Rationale:

- Tests read as scenarios, not as `every { … } returns …` choreography.
- Fakes don't break when a signature changes; mock setups do.
- A fake makes it impossible to accidentally assert on *interactions* when the thing that
  matters is *behaviour* — which is the most common way a mock-heavy suite becomes a
  liability while still being green.

**Fixtures come from two places:**

1. **The real payload**, committed verbatim as a test resource and served by MockWebServer.
   Mapper tests therefore run against genuine data — the `null` labels, the Word-mangled
   HTML, `"Court Blue/Moonstone Blue/White"` — not a tidied approximation of it.
2. **Kotlin fixture builders** with sensible defaults, so each test names only the field it
   cares about: `productFixture(labels = listOf(Label.GoingFast), compareAtPrice = null)`.
   Edge-case tests stay readable and the intent of each is obvious at a glance.

Malformed and truncated bodies are declared inline in the tests that use them rather than
as separate resource files — the corruption is the subject of the assertion, so it belongs
where it can be read.

**Practical constraint:** JUnit 5 covers JVM unit tests, but Android instrumented tests
still run on JUnit 4. The suite is split accordingly — `:core:model`, `:core:data` and
ViewModel tests on JUnit 5; Compose UI tests on JUnit 4. Normal, and better decided now than
discovered when `createAndroidComposeRule` fails to compile.

### 9.1 Unit — JUnit5, Turbine, MockWebServer, kotlinx-coroutines-test

| Target | Assertions |
|---|---|
| `ProductMapper` | Nullable `fit`/`labels`/`compareAtPrice`; multi-value `colour` on both `/` and `\|` separators; missing `featuredMedia`; empty `media` |
| `HtmlSanitiser` | The exact real description string → expected clean output. Golden-file regression |
| `Money` | `fromMajorUnits` conversion, locale formatting, zero, discount comparison, the reversed minor-units interpretation |
| `Label` | Known parsing across both categories, case-insensitivity, `Unknown` fallback, null and empty arrays (both meaning no labels), at-most-one-merchandising-badge on a constructed multi-label fixture |
| `ProductListViewModel` | Loading→Content, Loading→Empty, Loading→Error per `ErrorCause`, retry, refresh-without-blanking. Asserted with Turbine |
| Repository | Cache hit avoids a second network call; error mapping |

**MockWebServer carries the weight of §2.7.** The real endpoint cannot fail, so failure is
proved here: HTTP 500, socket timeout, malformed JSON body, `{"hits": []}`, and a truncated
response. Each asserted end-to-end through repository → ViewModel → the correct `UiState`.

`MainDispatcherRule` and an injected `CoroutineDispatcher` — no `Dispatchers.IO` hardcoded
anywhere, which is what makes any of this testable.

### 9.2 Snapshot — Paparazzi

JVM-only, no emulator, runs in CI on every PR. Goldens for:

- Each `UiState` branch: Loading, Content, Empty, each `ErrorCause`
- Product card: with label, without label, `Unknown` label, on-sale (strikethrough)
- **Image error placeholder and image loading placeholder**
- Long title / long colour string truncation
- Light and dark theme
- Font scale 1.0 and 2.0 (accessibility regression — the one people skip)
- Detail screen with the real sanitised HTML rendered

### 9.3 Instrumented — two Compose UI tests

The Robolectric layer is **cut for budget** (see `SCOPE.md` §2). Paparazzi covers rendering
in every state; the unit suite covers behaviour. What remains is the narrow set that neither
can honestly verify:

1. Tapping a product navigates to the correct detail screen; system back returns to the
   list with scroll position preserved.
2. The error state's Retry action recovers to `Content`.

`createAndroidComposeRule`, with `FakeProductRepository` injected via `@TestInstallIn`.
Compose's testing API rather than raw Espresso, since the UI is entirely Compose. JUnit 4,
per §9.0.

- List renders, scrolls, and click → detail navigation with the correct product
- System back returns to the list with scroll position preserved
- Error state → Retry → Content
- Semantics / content descriptions present on images and badges (talkback-testable)

### 9.4 Proving robustness the endpoint can't

- **The MockWebServer suite above.**
- **One fixture product's `featuredMedia.src` deliberately points at a dead path**, so the
  error placeholder is visible on first launch. The reviewer sees the requirement met
  without opening a test file.

### 9.5 Coverage

**Kover, reported, with no enforced threshold.** The figure for `:core:model` and
`:core:data` is quoted in the README alongside a plain statement of what is intentionally
uncovered. A gate would invite tests written to move a number rather than to catch a defect,
which is precisely the failure this suite is trying to avoid demonstrating.

### 9.6 Deliberately not tested

Stated in the README, because naming this is judgement rather than omission:

- Data class getters, `copy`, `equals` — testing the compiler.
- Compose framework internals; Coil's own loading and caching behaviour.
- Hilt graph construction — a compile-time concern, verified by the build.
- Theme token values in isolation, beyond the automated contrast check over token pairs.
- Exhaustive permutations of Paparazzi goldens. The matrix in `DESIGN.md` §8 covers the
  states that can actually break; every additional golden is a file to review on every
  visual change, and a suite nobody wants to update is a suite that gets deleted.

---

## 10. Data layer

**Single fetch plus in-memory cache**, exposed as `Flow<Result<List<Product>>>`.

The endpoint is one static file with no cursor and no page tokens. Building Paging 3 over it
would mean writing a `PagingSource` that slices an already fully-loaded list, and every
piece of the `RemoteMediator` / invalidation machinery would be dead code — testing our own
fake. The position to defend:

> Paging 3 is the right answer at hundreds of items against a cursor-based API. Here the
> endpoint returns a complete 60-item response with no paging mechanism behind it, so
> paging would be ceremony without substance. The repository returns a `Flow` of the full
> list; substituting a `PagingSource` later is a change behind one interface.

`Room` was likewise rejected: a schema, DAOs and migrations for sixty products that never
change reads as over-engineering rather than rigour. The repository interface makes
persistence an additive change if requirements grow.

**Stack:** Retrofit + OkHttp + kotlinx.serialization (`ignoreUnknownKeys = true` — the
Algolia envelope carries far more fields than we model, and unknown-key tolerance is what
stops an upstream addition from crashing the app).

Detail screen receives **only the product id** and reads from the cached repository. A whole
`Product` — with 7 media objects and a 2KB HTML description — must not be serialised through
the back stack.

### 10.1 Two behaviours that must be specified, not inferred

- **Pull-to-refresh bypasses the cache.** `refresh()` forces a network fetch and replaces
  the cached list; `getProducts()` serves from cache when populated. Without this the
  refresh gesture is decorative. On refresh failure the **existing content is retained** and
  the error surfaces transiently — never replace a populated list with an error screen.
- **Refetch succeeds but the id is absent.** After process death the detail screen refetches
  and the requested product may no longer be in the response. This resolves to
  `UiState.Error(ErrorCause.NotFound)` with a message offering a return to the list — **not**
  a crash, not an infinite spinner, not an empty detail screen. Add `NotFound` to
  `ErrorCause` and cover it with a fake whose refetch returns a list without the id.

---

## 11. Navigation — Navigation 3

Navigation 3 reached **stable 1.0.0 in November 2025** (1.0.1, February 2026). Google's
guidance is that Nav3 is the Compose-first library and that existing Navigation Compose
users should consider migrating. For a greenfield Compose app in 2026, Nav3 is the default
and Nav2 is the choice requiring justification.

What it buys us specifically here:

- The back stack is **ordinary Compose state** — assertable in tests, not hidden inside an
  opaque `NavController`.
- Predictive back works correctly.
- Adaptive list-detail (tablet / foldable two-pane) is first-class, not bolted on.

Hilt integration is straightforward — `hiltViewModel()` still works, provided
`rememberViewModelStoreNavEntryDecorator()` is added to `NavDisplay`'s `entryDecorators` so
ViewModels are scoped to the `NavEntry` and cleared on pop:

```kotlin
NavDisplay(
    backStack = backStack,
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
    ),
    entryProvider = entryProvider { … },
)
```

Routes are `@Serializable` keys (`data object ProductList`, `data class ProductDetail(val id: String)`).

**Adaptive two-pane layout is cut** (see `SCOPE.md` §2 and Appendix A). Phone layout only.

---

## 11.2 State restoration and process death

Three separate lifetimes, handled separately rather than conflated:

| Lifetime | Mechanism | What survives |
|---|---|---|
| Recomposition | `remember` | Transient UI state |
| Configuration change | `ViewModel` | Loaded products, `UiState` |
| **Process death** | `SavedStateHandle` + `rememberSaveable` | Selected size, scroll position, route arguments |

- **Selected size** lives in `SavedStateHandle`, not `remember`. It is user intent, and
  losing it on a low-memory kill is exactly the small breakage that makes an app feel
  cheap.
- **Grid scroll position** via `rememberLazyGridState()` with `rememberSaveable`.
- **The detail screen must survive an empty cache.** After process death the route argument
  is restored but the in-memory repository cache is gone. The detail ViewModel therefore
  asks the repository for a product by id and the repository refetches on a miss — the
  screen shows `Loading`, then `Content`. Restoring to a crash or a blank screen here is
  the single most common failure of this pattern, so it is explicitly tested with a
  fake repository whose cache starts empty.

Because the detail screen carries **only the product id** (§10), there is nothing large to
serialise into the saved state bundle — which is the other half of why that decision was
taken.

---

## 12. Images — Coil 3

`GsAsyncImage` in `:core:designsystem` is a **thin wrapper around `coil3.compose.AsyncImage`**,
not a replacement for it. The wrapper exists because Coil's built-in `placeholder`/`error`
parameters take *painters*, and none of the following can be expressed as a painter:

- A **composed** error fallback (brand surface, mark, product initial) rather than a grey box.
- An aspect ratio reserved from the payload's `width`/`height` before load.
- A `contentDescription` fallback from title when `alt` is null (which it is, on every product).
- A single deterministic seam for Paparazzi to force the loading and error states.

**`AsyncImage` with an `AsyncImagePainter` state listener — explicitly not
`SubcomposeAsyncImage`.** Subcomposition is measurably more expensive in a scrolling grid,
and given the performance goals in §14 this is designed in from the start rather than
measured out later. The wrapper's public API is identical either way, which is itself part
of the argument for having a wrapper.

- **Three distinct visual states**: shimmer placeholder while loading, a branded error
  fallback (icon plus product initial — not a grey box), and the image.
- **Aspect ratio reserved from the payload's `width`/`height`** before load, so the grid
  never jumps.
- `crossfade` for perceived smoothness; explicit **size hints** so a 1692×2018 JPEG is not
  decoded at full resolution into a grid cell.
- `contentDescription` from `alt` where present, falling back to the product title —
  `alt` is `null` throughout this payload, so the fallback is the real path.

---

## 13. UI direction

Gymshark-flavoured: bold, dark, editorial. Material 3 components and semantics underneath
(accessibility, touch targets, predictive back) with brand tokens layered on top — not
default M3 with dynamic colour, which would look like every other submission and like
nothing the brand ships.

Detailed tokens and screen layouts follow in the design document.

---

## 14. Performance

**Goal: no perceptible jank while scrolling the product grid, proven with measurement data
from a physical device.**

### 14.1 What we measure, and why only this

Performance data serves three separate purposes on a real team, and only one of them
applies to a take-home:

| Loop | Tooling | Applies here? |
|---|---|---|
| **Field monitoring** — what users actually experience; drives prioritisation | Play Console Android Vitals (slow frames >16ms, frozen frames >700ms — Play can demote discoverability for breaching thresholds), plus RUM via Firebase Performance / New Relic / Sentry | **No.** No users. |
| **Regression prevention** — did this change make it worse? | Macrobenchmark in CI as a *differential* gate on pinned hardware | **No.** Single contributor, ~30 commits. A trend line needs a trend. |
| **Investigation** — why is this specific thing slow? | Perfetto trace, one-off | **Yes.** This is the loop that demonstrates skill. |

A trend dashboard on a repository with one contributor is scaffolding for a process with
nothing to process, and an experienced reviewer may read it as not understanding why the
tooling exists. What earns marks is the **measure → diagnose → fix → re-measure loop,
demonstrated once, honestly.** `PERFORMANCE.md` documents that loop; it also states in one
line what would be done in production, so the knowledge is on record without the theatre.

### 14.2 Method

All measurement on a **physical device, release build** — emulator and CI-runner numbers
are noise and are not published.

- **Macrobenchmark** module with `FrameTimingMetric` (`frameOverrunMs`,
  `frameDurationCpuMs` at P50/P90/P95/P99 — positive overrun means a dropped frame) and
  `StartupTimingMetric`.
- **Baseline Profile** generated via `BaselineProfileRule`. The headline evidence is a
  `CompilationMode.None()` vs `CompilationMode.Partial()` comparison — Baseline Profiles
  give roughly **30% faster code execution from first launch** by letting ART compile the
  profiled paths AOT instead of interpreting and JIT-ing them. For a scroll-heavy list app
  this is the single largest lever.
- **Perfetto traces** — Macrobenchmark emits `.perfetto-trace` files alongside
  `benchmarkData.json` automatically. Two are committed to the repo (before and after one
  specific fix) with instructions to open them at `ui.perfetto.dev`. Note that Perfetto's
  UI is a client-side viewer: traces are processed locally in the browser and are not
  uploaded anywhere.

### 14.3 Design decisions taken *for* performance, not retrofitted

- `AsyncImage`, not `SubcomposeAsyncImage` (§12).
- `LazyVerticalGrid` items carry a stable `key` (product id) and `contentType`, so Compose
  reuses slots correctly instead of rebuilding them.
- **The ViewModel emits fully-mapped immutable UI models.** Sanitising, currency formatting
  and label mapping happen in the mapper. `AnnotatedString.fromHtml` is the sole exception
  (§6) and is memoised with `remember`, so it runs once per description, never per frame. Every
  per-frame allocation in a scroll is jank that was chosen rather than suffered.
- UI models are `@Immutable` with `ImmutableList` (kotlinx.collections.immutable) rather
  than `List`, so Compose can skip recomposition instead of assuming instability.
- Image aspect ratio reserved from payload `width`/`height` — no layout jump, no
  re-measure pass mid-scroll.
- Explicit Coil size hints so a 1692×2018 JPEG is not decoded at full resolution into a
  grid cell.
- R8 full mode enabled on release.

### 14.4 Compose compiler stability reports

Compiler metrics enabled behind a Gradle property. The report is read and acted on —
unstable parameters, unnecessary recompositions, unskippable composables — and
`PERFORMANCE.md` records what it flagged and what was changed. "Here is the report, here is
what I fixed" is a stronger artefact than a clean report with no evidence of having looked.

### 14.5 Explicitly out of scope, and why

- **Macrobenchmark in CI** — GitHub Actions has no physical devices; emulator numbers on
  shared runners are too noisy to be a meaningful gate on a project this size.
- **GitHub Pages trend dashboard / New Relic** — see §14.1. Recorded in `PERFORMANCE.md`
  as what production would use, not built.
- **JankStats** — field-monitoring tool (still `1.0.0-beta01`); answers a question this
  project doesn't have.

---

## 15. Open assumptions, stated for the record

1. `price` / `compareAtPrice` are in major units, not minor units (§7) — reversed from the
   original assumption once the real payload's price distribution (54×£1,000, plus £50,
   £60×4, £65) made the minor-units reading implausible and the major-units reading match
   real Gymshark pricing.
2. Currency is GBP; formatting is locale-aware and would follow a real currency code if the
   API supplied one.
3. The six observed label values split into merchandising and sustainability categories
   (§8); an unrecognised future value defaults to the merchandising `Unknown` treatment.
4. `discountPercentage` is displayed as supplied, never recalculated. `compareAtPrice` and
   `discountPercentage` are `null` on every product in this payload, so the discount
   treatment ships implemented and fixture-tested but not live-reachable — kept rather than
   cut, since it is genuine nullable-field handling for a real schema field, not an
   affordance invented from nothing (unlike "Add to bag", which has no schema backing at
   all). Stated plainly in the README rather than left for a reviewer to notice.
5. The endpoint is treated as read-only and unauthenticated.

---

## Appendix A — designed, then cut

Recorded because the reasoning is worth keeping and the interview will ask. **None of the
following is to be built.** See `SCOPE.md` for the 20-hour budget that drove each decision.

| Cut | What it was | Why it went |
|---|---|---|
| **Eight-module structure** | Separate `:core:network`, `:feature:productlist`, `:feature:productdetail` | Four extra build files for no additional boundary argument |
| **Adaptive two-pane layout** | `ListDetailPaneScaffold` on `WindowSizeClass`, list and detail side by side on tablets and unfolded foldables | Genuinely valuable and genuinely 3–4 hours with goldens and device verification |
| **Robolectric UI-test layer** | JVM-hosted Compose tests as the bulk of UI coverage | Paparazzi covers rendering, unit tests cover behaviour; the middle layer was the affordable loss |
| **Colourway grouping** | One card per title with colour swatches; the sixty hits are twenty-one products | Would mean inventing a product model the search API does not express |
| **Disk cache** | Room or Coil disk persistence for offline first launch | Schema, DAOs and migrations for ten products that never change |
| **Perfetto investigation write-up** | A found-and-fixed jank source with before/after traces | Requires a real defect to find; Baseline Profile before/after numbers are kept |
| **CI Macrobenchmark** | Benchmarks as a regression gate in GitHub Actions | No physical devices on hosted runners; emulator figures too noisy to gate on |
