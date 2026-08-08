# Design — build the Gymshark product catalogue app

## Context

See `proposal.md` for motivation. The full decision record, including every rejected
alternative, already exists in `docs/ARCHITECTURE.md`; this document does not restate it. What
follows is the design-level guidance an implementer needs on top of those documents: the
shape of the pipeline, the four decisions that were resolved late and are therefore not yet
muscle memory in the repository, and the risks that will actually bite within the 20-hour
budget.

Three constraints shape everything below:

1. **The endpoint cannot fail.** It is a static CDN file, so every failure path the challenge
   asks us to handle exists only behind a deliberate test seam.
2. **The budget is 20 hours against a 35–50 hour design.** `docs/SCOPE.md` is the cut and takes
   precedence over every other document.
3. **Three items are graded explicitly** — the HTML description, broken and missing images,
   and label indicators. Everything else is tradeable.

Precedence when documents disagree: `SCOPE.md` → `ARCHITECTURE.md` (main body; Appendix A is a
record of cuts) → `DESIGN.md` → `CONVENTIONS.md`.

## Goals / Non-Goals

**Goals:**

- A pipeline in which every transformation that can be a pure function *is* one, so the
  highest-risk logic is testable on a plain JVM in milliseconds.
- Failure paths proven end to end against a mock server, since the real endpoint cannot
  demonstrate them.
- Module boundaries that constrain rather than decorate — features cannot reach each other,
  and the domain module cannot reach Android.
- Performance decisions taken before the code exists, not retrofitted after a benchmark.

**Non-Goals:**

- Persistence of any kind. No disk cache, no database, no favourites.
- Any adaptive or multi-pane layout. Phone portrait and landscape only.
- Pagination. The endpoint returns a complete ten-item response with no cursor.
- Production observability. `docs/PERFORMANCE.md` §7 records what it would be; none of it is
  built.

## Decisions

### 1. The HTML pipeline splits at the Android boundary

This is the decision most likely to be got wrong, because the obvious placement is the wrong
one. `AnnotatedString.fromHtml` is a Compose API backed by `android.text.Html` and cannot run
in a JVM unit test — but sanitising, the part that carries all the risk, can.

```
raw payload description  (Word artefacts, <meta> mid-body, <br> pseudo-lists)
        │
        ▼
   HtmlSanitiser.sanitise()          :core:model  ─ pure, JUnit 5, golden-file test
        │                                           NO Android, NO Compose
        ▼
   SanitisedDescription(
       heading: String?,             ← extracted from the leading <strong>-only <p>
       bodyHtml: String,             ← <p>, <strong>, <em>, <br>, <ul>, <li>, <a> only
   )
        │
        ▼
   ProductDetailUiModel              :feature:products  ─ plain strings, JUnit 5 testable
        │
        ▼
   remember(bodyHtml) {              the composable  ─ Android runtime available here
       AnnotatedString.fromHtml(…)   once per description, never per frame
   }
        │
        ▼
   Text(heading, style = eyebrow)  +  Text(annotated, style = body)
```

**Why the heading is a separate field rather than markup.** Emitting `<h1>`–`<h6>` would work —
Android's parser produces a `RelativeSizeSpan` that `fromHtml` does translate — but its size
multipliers would silently override the type scale in `docs/DESIGN.md`. Returning the heading
as a string lets it render with the `eyebrow` token like every other piece of text in the app.

**Why the memoisation matters.** `docs/PERFORMANCE.md` §6 claims nothing is computed during
composition. With this split the honest claim is *nothing is computed per frame* — the
description converts once, keyed on its own string. The expensive, hostile, regression-prone
half stays pure and JVM-tested. `docs/ARCHITECTURE.md` §6 records this in full.

**Alternatives rejected:** a full block-model parser producing a `DescriptionBlock` hierarchy
(maximum control, more machinery than this content justifies); putting `fromHtml` in the
ViewModel (would drag Robolectric back into the ViewModel test layer, which `SCOPE.md` §2 cut);
`AndroidView` + `HtmlCompat` (a Compose-hostile island, awkward to snapshot).

### 2. Bullets depend on a library version floor, and that is verified before the screen is built

Compose `ui-text` before 1.9 had no representation for a bullet: Android's parser emits a
`BulletSpan` for `<ul>/<li>` and there was no `AnnotatedString` equivalent, so lists rendered
as unmarked lines. `ui-text` 1.9 added `Bullet`, `BulletScope` and `withBulletList`, and
`fromHtml` now emits them.

This is load-bearing and cheap to get wrong quietly, so the sequencing is deliberate:

- The Compose BOM is pinned in task 1, and the resolved `ui-text` version is checked then —
  not discovered on day 3.
- Bullet rendering is proved by a Paparazzi golden **before** the detail screen is built out.
- If bullets do not render, that is a version-floor defect to raise. It is explicitly **not**
  to be worked around by emitting `•` characters as body text — that would satisfy a
  screenshot while failing the requirement.

### 3. State is a closed set per screen, and errors are typed causes

```
ProductListUiState                     ProductDetailUiState
├── Loading                            ├── Loading
├── Content(products, isRefreshing)    ├── Content(product)
├── Empty                              └── Error(cause)
└── Error(cause)

ErrorCause: NoConnection │ Server │ Malformed │ NotFound │ Unknown
```

Two placements carry weight:

- **`isRefreshing` lives inside `Content`**, not beside it. That is what makes it structurally
  impossible for a pull-to-refresh to blank the list the user is reading.
- **`NotFound` is an `ErrorCause`**, not a fourth detail state. After process death the route
  argument survives but the cache does not; a refetch can legitimately return a payload without
  the requested id. That resolves to an error with a return-to-list action — the alternative,
  an infinite spinner or a blank screen, is the classic failure of this pattern.

A raw `Throwable` never enters presentation state; that would leak the data layer into the UI
and make the "you're offline" versus "we broke" distinction impossible.

### 4. One repository, two read paths, one cache

```
ProductRepository            @Singleton — outlives the list screen so detail can read it
├── getProducts()            cache when populated, else network
├── refresh()                always network; replaces cache; on failure the caller keeps
│                            its existing content and surfaces the error transiently
└── getProduct(id)           cache hit → return; cache miss → refetch → look up again
                             found → Content;  absent → Error(NotFound)
```

Refresh bypassing the cache is what stops the pull gesture being decorative, and is specified
rather than inferred because it is the kind of thing that reads as working while doing nothing.

Detail navigation carries the **product id only**. A whole product — seven media objects and a
two-kilobyte description — must not be serialised through the back stack, which is also why
there is nothing large in the saved-state bundle after process death.

Paging 3 and Room are both rejected in `docs/ARCHITECTURE.md` §10 with reasoning worth reading
before anyone reaches for either.

### 5. Failure is proved through a seam, and visible without opening a test

Two complementary mechanisms, because they answer different audiences:

| Mechanism | Proves | Audience |
|---|---|---|
| MockWebServer suite — 500, timeout, malformed, truncated, `{"hits":[]}` | Every typed cause, end to end through repository → ViewModel → state | A reviewer reading the test suite |
| One fixture product pointing at a dead image URL | The composed image error state, on first launch | A reviewer who just runs the app |

The second is the cheaper of the two and does more work. The graded requirement is "handle
incorrect and/or missing images"; making that visible without instruction is the difference
between claiming it and demonstrating it.

### 6. Three lifetimes, handled separately

Conflating these is the usual source of "it works until you rotate it".

| Lifetime | Mechanism | Carries |
|---|---|---|
| Recomposition | `remember` | The memoised `AnnotatedString`, transient UI state |
| Configuration change | `ViewModel` + `WhileSubscribed(5_000)` | Loaded products, current state — the stop timeout is what prevents a rotation re-triggering the fetch |
| Process death | `SavedStateHandle`, `rememberSaveable` | Selected size, grid scroll position, route arguments |

Selected size goes in `SavedStateHandle` rather than `remember` because it is user intent, and
losing it to a low-memory kill is exactly the small breakage that makes an app feel cheap.

### 7. The image wrapper exists for four things a painter cannot express

`GsAsyncImage` wraps `AsyncImage` — deliberately not `SubcomposeAsyncImage`, whose
subcomposition cost is measurable in a scrolling grid. Coil's own `placeholder`/`error`
parameters take painters, and none of these can be a painter: a composed error state built
from tokens, an aspect ratio reserved from payload `width`/`height` before load, a
`contentDescription` falling back to the title when `alt` is null (which it is on every
product), and a deterministic seam for Paparazzi to force each state without a network.

**All three states share one shape instance.** A radius mismatch produces a corner pop on every
image that resolves mid-scroll — precisely the defect class the "error free experience"
criterion is testing.

### 8. Testing splits on the runtime boundary, and doubles are hand-written

JUnit 5 for JVM unit tests — domain, data and ViewModels, the last requiring the approved
`de.mannodermaus.android-junit5` plugin because AGP will not otherwise run JUnit 5 in an
Android library module. JUnit 4 for the two instrumented tests, because `createAndroidComposeRule`
requires it. Paparazzi in between, JVM-hosted, no emulator.

Hand-written fakes throughout, no mocking library: fakes make tests read as scenarios, survive
signature changes, and make it structurally awkward to assert on interactions when behaviour is
what matters.

### 9. The benchmark measures a harness, and says so

Ten products in a two-column grid is five rows — there is not enough content to fling, so frame
timing over it would be noise presented as evidence. A `benchmark` build variant repeats the
committed fixtures to ~500 items to produce a real scroll.

This is the one place the measured artefact differs from the shipped one, so every table using
it is labelled. Startup timing and the Baseline Profile comparison run against the real app and
carry no caveat.

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| **`ui-text` 1.9's bullet support does not behave as documented** → the graded requirement fails late | Verified in task 1 when the BOM is pinned, and pinned by a golden before the detail screen is built. Raise it; do not fake bullets with text characters |
| **`HtmlSanitiser` consumes an afternoon** → day 1 overruns into the hard parts | Scheduled day 1 while there is room to absorb it. Timebox 2h; if it overruns, ship a simpler sanitiser and state the limitation |
| **Convention plugins fight back** → build-logic eats the foundation budget | Timebox 2h. Fallback is plain build files across six modules — uglier, not fatal |
| **Paparazzi/JDK friction** → snapshot layer unavailable | Known rough edge. If unresolved in 45 minutes, drop snapshot testing and say so; the unit suite carries the grade |
| **The Paparazzi matrix exceeds its 1.5h** → goldens crowd out the detail screen | Tier 1/Tier 2 split is pre-committed in `docs/DESIGN.md` §8. Record Tier 1 from the start; Tier 2 only if time remains |
| **Navigation 3 APIs differ from those recorded** → nav wiring stalls on day 3 | Verify the published artifacts, including `lifecycle-viewmodel-navigation3`, when the catalog is written on day 1 rather than when nav is wired |
| **Six modules for two screens** | Accepted deliberately. The brief is a demonstration of structural judgement, and the convention-plugin setup is part of the answer |
| **The minor-units assumption is wrong** | Single named constant, both interpretations covered by tests, assumption stated in the README. A one-line reversal |
| **Day 3 runs over** | Cut in the order fixed by `docs/SCOPE.md` §5: benchmark scroll → instrumented tests → Paparazzi matrix → CI. Never the HTML rendering, image error state, label indicators, unit suite or README |

**Accepted trade-off — no offline first launch.** Without a disk cache, a first launch with no
connectivity shows the error state rather than content. Correct for ten products that never
change; recorded in the README as deferred.

**Accepted trade-off — the list ships flat.** The ten hits are five products in ten colourways
and `handle` encodes it. Grouping would mean inventing a product model the search endpoint does
not express. One README line converts a possible oversight into visible judgement.

## Open Questions

None blocking. Three documentation inconsistencies remain and should be cleaned up in the
`docs:` commit rather than allowed to reach a reviewer:

- `docs/CONVENTIONS.md` §2 still lists Robolectric in the `gymshark.android.test` convention
  plugin, and §5 still lists it in the CI pipeline. The layer is cut.
- `docs/ARCHITECTURE.md` §9.3 is headed "two Compose UI tests" above a four-bullet list.
- `README.md`'s testing table describes snapshot coverage as "every `UiState`, every label
  treatment", which describes the full matrix rather than the Tier 1 set that will exist unless
  time allows Tier 2.
