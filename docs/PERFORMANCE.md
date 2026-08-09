# Performance

> ## ⚠️ STOP — instruction for any agent or contributor
>
> **Every `TBC` in this file must remain `TBC` until a human runs the benchmark on a
> physical device.**
>
> Do not estimate, infer, extrapolate, or generate plausible-looking figures. Do not fill
> these in from typical values, from documentation, or from an emulator run. Fabricated
> performance data in a submission is worse than no performance data — it is the one thing
> here that would be actively dishonest.
>
> Your job is to build the benchmark module so these numbers *can* be produced. Filling
> them in is not your job.

**Template.** Every number in this document comes from a physical device running a release
build. No emulator or CI-runner figures are published here; they are too noisy to be
meaningful.

---

## 1. Goal

No perceptible jank while scrolling the product grid, and a fast first launch — both
demonstrated with measurement data rather than asserted.

---

## 1a. Dataset — no synthetic harness

The real payload contains **60 products**: thirty rows in a two-column grid, a genuine
sustained scroll. An earlier version of this document planned a `benchmark` build variant
that repeated the fixtures to ~500 items, on the assumption that the payload held only ten
products (five rows — not enough to fling). That assumption was wrong; the multiplication
step is unnecessary and was dropped.

Every figure in this document, including the scroll numbers, is measured against the real
shipped dataset. There is no separate harness variant and no "measurement harness, not
production behaviour" caveat to attach to any table below.

---

## 2. Test conditions

| | |
|---|---|
| Device | Pixel 9 Pro, Android 17 (API 37) |
| Build | `release`, R8 full mode, minified |
| Iterations | 10 |
| Benchmark module | `:macrobenchmark` |
| Metrics | `FrameTimingMetric`, `StartupTimingMetric` |
| Baseline Profile | Generated via `BaselineProfileRule` |

Device state: connected via USB, screen on, no other foreground activity during the run.
`MacrobenchmarkRule` runs its own compilation step ahead of each `CompilationMode.Partial()`
iteration set; no additional manual warm-up/discard pass was layered on top of that.

---

## 3. Headline result — Baseline Profile impact

Product grid scroll, 10 iterations.

| Metric | `CompilationMode.None()` | `CompilationMode.Partial()` | Change |
|---|---|---|---|
| `frameOverrunMs` P50 | -10.91 ms | -11.15 ms | -0.24 ms |
| `frameOverrunMs` P90 | -9.29 ms | -9.74 ms | -0.45 ms |
| `frameOverrunMs` P95 | -8.55 ms | -9.15 ms | -0.60 ms |
| `frameOverrunMs` P99 | -6.77 ms | -7.67 ms | -0.90 ms |
| `frameDurationCpuMs` P95 | 5.96 ms | 5.64 ms | -0.32 ms |
| Dropped frames (overrun > 0) | 1 / 2429 (0.04%) | 3 / 2416 (0.12%) | +2 frames |

**Reading this table:** `frameOverrunMs` is how far past its deadline a frame landed.
Positive means the frame was dropped and the user saw a stutter; negative means the frame
finished early by that margin. P95 is the number that matters — P50 being fine while P95 is
positive is exactly what "mostly smooth but noticeably janky" feels like in the hand.

**What this run actually shows:** every percentile is negative in both compilation modes —
on this device, the real 60-product grid scroll never approaches its frame deadline, with
or without the profile. The Baseline Profile measurably tightens frame timing (P95 overrun
0.60 ms more negative, `frameDurationCpuMs` P95 down ~5%), but there was no jank to fix in
either mode. §4 records that finding rather than inventing an investigation that didn't
happen.

### Startup

| Metric | `None()` | `Partial()` | Change |
|---|---|---|---|
| `timeToInitialDisplayMs` median | 175.04 ms | 161.28 ms | -13.76 ms (-7.9%) |
| `timeToFullDisplayMs` median | not measured | not measured | — |

`timeToFullDisplayMs` has nothing to report either side: it's only emitted when the app
calls `reportFullyDrawn()`, and `MainActivity` never does.

Baseline Profiles let ART compile the profiled code paths ahead of time rather than
interpreting and JIT-ing them on first run. The measured effect here (-7.9% median
`timeToInitialDisplayMs`) is real but well short of the commonly-cited "~30%" figure for
larger, more complex apps — this is a small, single-screen-at-launch app, so there is
correspondingly less cold-start work for the profile to save.

---

## 4. Measure → diagnose → fix → re-measure

This section is the point of the document: not that the app is fast, but that the process
used to make it fast is sound.

### No investigation needed — the real measurement found no jank

The §3 headline numbers were the diagnosis step: every `frameOverrunMs` percentile,
including P99, is negative in both `CompilationMode.None()` and `CompilationMode.Partial()`
on the real 60-product dataset — no frame ever missed its deadline by a measurable margin,
and only 1–3 frames out of ~2,400 sampled per run showed any overrun at all (noise-level,
not a pattern). There was no symptom to chase, so there is no before/after fix to report
here — writing one up would mean inventing a bug that this measurement did not find, which
the warning at the top of this document exists specifically to prevent.

Two representative traces are committed under `docs/traces/` as evidence for the headline
numbers rather than as a before/after pair:

- `docs/traces/scroll-no-compilation.perfetto-trace`
- `docs/traces/scroll-baseline-profile.perfetto-trace`

> To inspect: open <https://ui.perfetto.dev> and drag the file in. Traces are processed
> locally in your browser — nothing is uploaded.

If a future change to the product grid (heavier per-item composition, more images in
flight, a longer list) reintroduces real jank, this is the section to fill in properly:
capture a fresh before/after trace pair, diagnose on the UI thread timeline, fix, and
re-measure.

---

## 5. Compose compiler stability report

Enabled with:

```
./gradlew assembleRelease -Pcompose.compiler.reports=true -Pcompose.compiler.metrics=true
```

| Finding | Location | Change made |
|---|---|---|
| `ErrorCause` (`:core:model`, a closed sealed interface of only `data object`s — genuinely immutable) inferred unstable | `ErrorState(cause: ErrorCause, …)`, `ProductListUiState.Error`, `ProductDetailUiState.Error` in `:feature:products` | Added `feature/products/compose_compiler_config.conf` declaring it stable, wired via `composeCompiler { stabilityConfigurationFiles }` in `feature/products/build.gradle.kts` — no Compose dependency added to `:core:model`, which stays a plain domain module (`docs/ARCHITECTURE.md` §3) |
| `viewModel: ProductListViewModel? / ProductDetailViewModel?` inferred unstable | `ProductListScreen`/`ProductDetailScreen` overloads in `:feature:products` | Not changed — expected/benign. Hilt `ViewModel`s have no equals contract so the compiler can never call them stable, but both composables are still reported `restartable skippable` (strong skipping mode compares by reference), and the `viewModel` parameter is never itself a recomposition trigger — only the `StateFlow` it exposes is read. |

Every other composable across `:core:designsystem`, `:feature:products`, and `:app` reports
`restartable skippable` with no other unstable parameters — the `ImmutableList`/`@Immutable`
UI-model pattern from §6 already accounted for the `List<T>` and lambda-capture classes of
finding before this report ran.

---

## 6. Decisions taken *for* performance rather than retrofitted

- `AsyncImage` rather than `SubcomposeAsyncImage` — subcomposition is measurably more
  expensive in a scrolling grid.
- Stable `key` and `contentType` on `LazyVerticalGrid` items, so Compose reuses slots.
- All mapping — HTML sanitising, currency formatting, label resolution — happens in the
  ViewModel mapper. The one exception is `AnnotatedString.fromHtml`, which needs an Android
  runtime and therefore runs in the composable behind `remember(bodyHtml)` — once per
  description, never per frame. See `ARCHITECTURE.md` §6.
- UI models are `@Immutable` and hold `ImmutableList`, so Compose can skip rather than
  assume instability.
- Image aspect ratio reserved from the payload's `width`/`height`, so the grid never
  re-measures mid-scroll.
- Explicit Coil size hints — a 1692×2018 source JPEG is never decoded at full resolution
  into a grid cell.
- R8 full mode on release.

---

## 7. What this would look like in production

Deliberately not built here, and the reasoning matters more than the artefact:

- **Field monitoring** would be the primary signal — Play Console **Android Vitals** (slow
  frames >16ms, frozen frames >700ms, both of which affect Play Store discoverability when
  they breach Google's bad-behaviour thresholds), supplemented by RUM through Firebase
  Performance Monitoring or New Relic with attribution by screen, device tier and OS
  version. That data answers "where should the next sprint go?", which is a question this
  project does not have.
- **Regression prevention** would run Macrobenchmark in CI on pinned hardware — ideally
  physical devices via Firebase Test Lab (requires Benchmark 1.3.2+ and `directoriesToPull`
  configured to retrieve results), with a threshold gate on P95 frame overrun.
- **A historical trend dashboard** (for example `benchmark-action/github-action-benchmark`
  publishing to GitHub Pages) becomes valuable once many contributors are merging many
  changes over months. On a repository with a single contributor it charts noise.

The tooling above is well understood; it is omitted because it would be process theatre at
this scale, not because it is unfamiliar.
