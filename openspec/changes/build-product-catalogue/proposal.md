# Build the Gymshark product catalogue app

## Why

The Gymshark Mobile Engineering Challenge (v2) asks for an Android app that parses a supplied
Algolia payload, presents it as a product list with images and label indicators, and shows a
detail screen whose HTML description is rendered appropriately. The repository currently holds
the decisions — architecture, design, scope, conventions and performance method — but no code.

This change turns those agreed decisions into behaviour contracts so that implementation can
proceed without re-litigating them, and so that the three explicitly graded requirements
(HTML description, broken/missing images, label indicators) are each backed by testable
scenarios rather than by prose in a design document.

## What Changes

- **A payload prerequisite is established.** The response from
  `https://cdn.develop.gymshark.com/training/mock-product-responses/algolia-example-payload.json`
  is fetched once and committed verbatim to `:core:testing` resources. Every mapper test, the
  sanitiser golden, the broken-image fixture and the benchmark dataset depend on it.
- **A pure domain model.** `Product`, `Money` (stored internally as minor units, never
  `Double`, mapped from the payload's major-unit values), `Label` split into merchandising
  and sustainability categories with an `Unknown(raw)` fallback, and multi-value colour
  normalisation across both payload separators.
- **A data layer.** Retrofit + kotlinx.serialization DTOs, a mapper tolerant of the payload's
  nulls, an in-memory-cached repository, refresh that bypasses the cache, and typed error
  causes including `NotFound`.
- **HTML description rendering without a WebView.** A pure sanitiser returning a heading and a
  body HTML string, rendered through `AnnotatedString.fromHtml` on a pinned `ui-text` 1.9+
  floor so `<ul>/<li>` produce real bullets.
- **Composed image states.** Loading, loaded and error share one shape; aspect ratio is
  reserved from payload dimensions; `contentDescription` falls back to the product title.
- **Two screens.** A two-column product grid with typed states, pull-to-refresh that never
  blanks content, and scroll restoration; a detail screen with hero, thumbnails, size chips,
  selected-size survival across process death, and the rendered description.
- **A design system with verified accessibility.** Semantic tokens, a WCAG AA contrast test
  written before the tokens are used, and RTL support.
- **Quality and performance evidence.** Static analysis gates, a hand-written-fakes test
  strategy, a Macrobenchmark harness, and an absolute prohibition on fabricated figures.

Nothing outside the two screens ships: no bottom navigation, add-to-bag, favourites, search,
sort, filter, analytics or splash screen. Adaptive two-pane, colourway grouping, disk cache,
a Robolectric layer, shared-element transitions and the Perfetto write-up are all cut and
recorded as deferred decisions.

## Capabilities

### New Capabilities

- `product-domain-model`: Domain types and their observable behaviour — money mapped from
  major units into an internal minor-units representation, label parsing across
  merchandising and sustainability categories with an unknown fallback, colour
  normalisation.
- `product-catalogue-data`: Fetching, parsing and caching the Algolia payload, refresh
  semantics, and the typed error causes surfaced to the UI.
- `html-description-rendering`: Sanitising hostile CMS HTML and rendering it as a heading,
  paragraphs and a real bullet list without a WebView.
- `product-imagery`: The three visible image states, reserved aspect ratio, and image
  accessibility — the graded "handle incorrect and/or missing images" requirement.
- `product-list-screen`: The product grid, its typed states, refresh, scroll restoration and
  label indicators.
- `product-detail-screen`: Detail content, size chips, state restoration across process death,
  and recovery when a refetched payload no longer contains the requested product.
- `design-system-and-accessibility`: Tokens, typography, contrast verification, RTL, touch
  targets and screen-reader semantics.
- `engineering-quality-gates`: Definition of done, test strategy, static analysis, performance
  measurement method, and the honesty constraints on published figures and screenshots.

### Modified Capabilities

None — this is a greenfield repository with no existing specs.

## Impact

- **New code:** six modules (`:app`, `:core:model`, `:core:data`, `:core:designsystem`,
  `:core:testing`, `:feature:products`) plus `:macrobenchmark` and a `build-logic` included
  build supplying convention plugins.
- **New committed assets:** the verbatim Algolia payload as a test resource, a Baseline Profile,
  and detekt/ktlint configuration. No snapshot goldens — Paparazzi's Gradle plugin is
  incompatible with the AGP version Hilt requires; see `design.md`.
- **Dependencies:** only those in `AGENTS.md` §4b. Notably `de.mannodermaus.android-junit5`
  (JUnit 5 in Android library modules) and a Compose BOM whose `ui-text` is 1.9 or later —
  the bullet-rendering floor. Adding anything else requires stopping and asking.
- **External call:** one read-only fetch of the CDN payload, performed once by a human and
  committed. The app itself treats the endpoint as read-only and unauthenticated.
- **Documentation:** `README.md`, `docs/PERFORMANCE.md` and `docs/DESIGN.md` carry placeholders
  (screenshots, `TBC` figures) that only a human may fill.
