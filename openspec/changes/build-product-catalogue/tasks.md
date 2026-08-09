# Tasks

Ordered so that if time runs out, what exists is coherent rather than half-wired. Groups map
to `docs/SCOPE.md` §3; the cut order if day 3 overruns is fixed in `docs/SCOPE.md` §5.

Every task ends at the definition of done in `AGENTS.md` §6 — compiles with warnings as errors,
static analysis clean, tests written and passing, no `!!`/`lateinit`/`TODO`, one Conventional
Commit scoped by module. There is no snapshot test layer (Paparazzi's Gradle plugin is
incompatible with the AGP version Hilt requires — see `design.md` §8), so "goldens recorded
where the change is visual" from the original definition of done does not apply; the two places
that would have relied on one (bullet rendering, RTL mirroring) are manual on-device checks
instead, called out explicitly below.

## 1. Prerequisite and foundation (day 1, ~2.25h)

- [x] 1.1 Fetch `https://cdn.develop.gymshark.com/training/mock-product-responses/algolia-example-payload.json` and commit it verbatim to `core/testing/src/main/resources/algolia-example-payload.json` — no trimming, reformatting or tidying. Blocks groups 2, 3 and 4
- [x] 1.2 Initialise the Gradle project with `gradle/libs.versions.toml` containing only the dependencies approved in `AGENTS.md` §4b
- [x] 1.3 Verify the pinned Compose BOM resolves `ui-text` to 1.9 or later, and record the resolved version. If it does not, stop — this is the bullet-rendering floor
- [x] 1.4 Verify the published `androidx.navigation3` and `lifecycle-viewmodel-navigation3` artifacts and API names match `docs/ARCHITECTURE.md` §11 before relying on them on day 3
- [x] 1.5 Add the `build-logic` included build with the five convention plugins from `docs/CONVENTIONS.md` §2. Timebox 2h; fallback is plain build files across six modules
- [x] 1.6 Create the six modules — `:app`, `:core:model`, `:core:data`, `:core:designsystem`, `:core:testing`, `:feature:products` — with `:core:model` as a pure JVM module and explicit API mode on `:core:*`
- [x] 1.7 Configure ktlint, detekt and Android Lint to fail the build, with no baseline file, and KSP for annotation processing

## 2. Domain model (day 1, ~1.5h)

- [x] 2.1 Add `Money` as a value class over `Long` minor units with a single named conversion constant and a `fromMajorUnits` factory
- [x] 2.2 Test `Money`: `fromMajorUnits(65)` → £65.00, `fromMajorUnits(1000)` → £1,000.00 (rendered as supplied, not special-cased), zero, locale formatting, and the reversed minor-units interpretation
- [x] 2.3 Add `Label` as a sealed interface with the six known values across two categories (merchandising: `going-fast`, `new`, `limited-edition`, `popular`; sustainability: `recycled-nylon`, `recycled-polyester`) and `Unknown(raw)` defaulting to merchandising
- [x] 2.4 Test `Label` parsing: known values in both categories, case-insensitivity, whitespace trimming, unknown fallback defaulting to merchandising, null and empty arrays (both meaning no labels), and the at-most-one-merchandising-badge rule on a constructed two-merchandising-label fixture
- [x] 2.5 Add the `Product` domain type and the pure colour-normalisation function handling both `/` and ` | ` separators
- [x] 2.6 Test colour normalisation against both payload separators and the single-colour case
- [x] 2.7 Add the discount rule: on sale only when `compareAtPrice` is present and strictly greater than `price`; `discountPercentage` displayed as supplied, never recomputed. Test all four branches against constructed fixtures — `compareAtPrice` and `discountPercentage` are `null` on every product in the real payload, so this logic has no live example

## 3. HTML sanitiser (day 1, ~2h — written test-first)

- [x] 3.1 Author the expected sanitised output for the committed payload's description **before** writing the implementation, as a golden test resource
- [x] 3.2 Define `HtmlSanitiser` returning `SanitisedDescription(heading: String?, bodyHtml: String)` in `:core:model`, with no Android or Compose dependency
- [x] 3.3 Implement and individually test: strip inline `<meta>`; strip `data-mce-*`; strip Word clipboard `class` attributes; collapse semantically empty `<span>` wrappers
- [x] 3.4 Implement and test extraction of a leading emphasis-only paragraph into `heading`, removed from the body
- [x] 3.5 Implement and test conversion of `<br>`-delimited `- ` runs into `<ul><li>`, with the leading hyphen and space removed from item text
- [x] 3.6 Implement and test whitespace and empty-paragraph collapsing
- [x] 3.7 Assert the full golden: the exact payload description string maps to the committed expected output
- [x] 3.8 Break the implementation once to confirm the golden test fails, then restore

## 4. Data layer (day 1–2, ~3.25h)

- [x] 4.1 Add Algolia envelope and product DTOs with `ignoreUnknownKeys = true`, plus the Retrofit service
- [x] 4.2 Add the DTO → domain mapper handling null `labels`, `fit`, `compareAtPrice`, `discountPercentage` and `alt`, missing `featuredMedia`, and empty `media`
- [x] 4.3 Test the mapper against the committed payload — all sixty hits map, and each null and multi-value case is asserted
- [x] 4.4 Add `ProductRepository` with an in-memory cache, `@Singleton`-scoped, exposing `getProducts()`, `getProduct(id)` and `refresh()`, with an injected dispatcher
- [x] 4.5 Implement and test `refresh()` bypassing the cache, and a cache hit avoiding a second network call
- [x] 4.6 Add `ErrorCause` — `NoConnection`, `Server`, `Malformed`, `NotFound`, `Unknown` — and map throwables to it
- [x] 4.7 Add the MockWebServer suite: HTTP 500, socket timeout, malformed body, truncated body and `{"hits": []}`, each asserted to the correct typed result
- [x] 4.8 Add `FakeProductRepository`, `MainDispatcherRule` and the fixture builders to `:core:testing`
- [x] 4.9 Point one fixture product's `featuredMedia.src` at a dead path so the image error state is visible on first launch

## 5. Design system (day 2, ~2h)

- [x] 5.1 Write the WCAG AA contrast test over every token pair **before** the tokens are consumed by any screen
- [x] 5.2 Add the colour tokens for light and dark from `docs/DESIGN.md` §1, including the corrected `textMuted` values, and make the contrast test pass
- [x] 5.3 Add the type scale, spacing scale and corner radii tokens; all text in `sp`, no elevation anywhere
- [x] 5.4 Add `GsAsyncImage` wrapping `AsyncImage` with loading, loaded and error states sharing one shape instance, aspect ratio reserved from payload dimensions, and explicit size hints
- [x] 5.5 Implement the `contentDescription` fallback: `alt` when present, product title otherwise
- [x] 5.6 Add `GsLabelBadge` for merchandising labels only, with the two visual tiers (urgency: going fast/limited edition; informational: new/popular) plus the quiet outlined `Unknown` treatment showing the raw value title-cased
- [x] 5.7 Add `GsMaterialChip` for sustainability labels, reusing the `success` token pair freed up now that "back in stock" isn't a real label
- [x] 5.8 Add `GsSizeChip` with selected, available and out-of-stock states, the out-of-stock state carrying a `stateDescription`
- [x] 5.9 Add `GsProductCard` with intrinsic height, two-line title clamp and one-line colourway clamp

## 6. Product list (day 2, ~2.5h)

- [x] 6.1 Add `ProductListUiState` with `Loading`, `Content(products, isRefreshing)`, `Empty` and `Error(cause)`, and `@Immutable` UI models holding `ImmutableList`
- [x] 6.2 Add `ProductListViewModel` exposing state via `stateIn(WhileSubscribed(5_000))`, mapping products in the ViewModel rather than in composition
- [x] 6.3 Test the state machine with Turbine: loading → content, loading → empty, loading → error per cause, retry recovery
- [x] 6.4 Test that refresh keeps content on screen while in flight and on failure
- [x] 6.5 Build the grid screen — wordmark, title, count, two-column `LazyVerticalGrid` with stable `key` and `contentType`, `safeDrawing` content padding, edge to edge
- [x] 6.6 Add pull-to-refresh, and the error state with its per-cause message and retry action
- [x] 6.7 Add scroll position restoration via `rememberSaveable`
- [ ] 6.8 Run the app on a device or emulator with the layout direction forced to right-to-left and confirm the list screen mirrors correctly — all padding/alignment via `start`/`end`, back and directional icons flipped. Manual check; no automated snapshot layer exists to pin it — **outstanding: needs a human on a device/emulator, cannot be done from this session**

## 7. Product detail (day 3, ~3h)

- [ ] 7.1 Add `ProductDetailUiState` with `Loading`, `Content` and `Error(cause)`, its UI model carrying `heading: String?` and `bodyHtml: String` as plain strings
- [ ] 7.2 Add `ProductDetailViewModel` resolving the product by id, with `SavedStateHandle` for the selected size
- [ ] 7.3 Test cache-miss refetch: empty cache → Loading → Content, using a fake whose cache starts empty
- [ ] 7.4 Test refetch succeeding without the requested id → `Error(NotFound)`, and refetch failing → the corresponding typed cause
- [ ] 7.5 Test that the selected size survives process death via `SavedStateHandle`
- [ ] 7.6 Build a minimal detail screen shell and render the description: `heading` in the `eyebrow` style, body via `remember(bodyHtml) { AnnotatedString.fromHtml(bodyHtml) }`
- [ ] 7.7 Run the app on a device or emulator and manually confirm the description renders real bullets with hanging indentation, **before** building out the rest of the screen. No automated test can check this — `AnnotatedString.fromHtml` needs an Android runtime and there is no snapshot layer (`design.md` §8). If bullets do not render, treat it as a version-floor defect; do not work around it with bullet characters as text
- [ ] 7.8 Build out the remaining screen: hero image with badge, thumbnail strip with selection outline, title, colourway, type, price
- [ ] 7.9 Add the size chip row driven by real per-size `inStock` data, out-of-stock chips disabled and unselectable
- [ ] 7.10 Add the material chip row (`GsMaterialChip` per sustainability label) after the hairline, before the description; shown independently of whether the hero badge is also showing a merchandising label. Verify against the one real product carrying `new` plus both recycled labels — badge and chips both present, no overflow handling needed

## 8. Navigation and wiring (day 3, ~0.5h)

- [x] 8.1 Add the Hilt application root and modules — `@Singleton` for `OkHttpClient`, Retrofit and `ProductRepository` — pulled forward into group 6, since `@HiltAndroidApp` already existed and `ProductListViewModel`'s `@Inject` constructor needed the binding to exist for `:app` to compile at all
- [ ] 8.2 Wire `NavDisplay` with `@Serializable` route keys, passing the product id only, and both entry decorators so ViewModels scope to the `NavEntry` and clear on pop
- [ ] 8.3 Verify predictive back, and that the back icon uses an `AutoMirrored` variant

## 9. Instrumented tests (day 3, ~0.5h — first cut after benchmark scroll)

- [ ] 9.1 Test: tapping a product navigates to the correct detail screen, and system back returns to the list with scroll position preserved
- [ ] 9.2 Test: the error state's retry action recovers to content, with `FakeProductRepository` injected via `@TestInstallIn`

## 10. Performance (day 3, ~1h)

- [ ] 10.1 Add the `:macrobenchmark` module with `FrameTimingMetric` and `StartupTimingMetric`, measured against the real sixty-product dataset — no synthetic `benchmark` build variant needed; thirty rows is a real scroll (see `design.md` §9)
- [ ] 10.2 Generate the Baseline Profile with `BaselineProfileRule`; enable R8 full mode on release
- [ ] 10.3 Run the Compose compiler stability report and act on what it flags
- [ ] 10.4 Leave every `TBC` in `docs/PERFORMANCE.md` untouched — a human runs the benchmark on a physical device and fills them in

## 11. Final pass (day 3, ~1.5h)

- [ ] 11.1 Add the CI workflow: ktlint → detekt → lint → JVM unit tests → `assembleRelease`, no secrets, no benchmarks
- [ ] 11.2 Fix the stale documentation — Robolectric in `docs/CONVENTIONS.md` §2 and §5, the "two tests / four bullets" mismatch in `docs/ARCHITECTURE.md` §9.3, and every Paparazzi/snapshot reference in `README.md`'s testing table and `docs/DESIGN.md` §8 (the layer is entirely absent, not reduced — see `design.md` §8 of this change for why)
- [ ] 11.3 Report Kover coverage for `:core:model` and `:core:data` and quote it in the README with the deliberately-untested list
- [ ] 11.4 Run a TalkBack pass on device and record the result in the README (the RTL pass already happened manually in task 6.8 — record that result too)
- [ ] 11.5 Remove the agent note comment from the README, and confirm no screenshot, performance figure or placeholder has been invented
- [ ] 11.6 Generate README screenshots from the running app on a device or emulator — human only. (Originally planned from committed Paparazzi goldens; there are none)
