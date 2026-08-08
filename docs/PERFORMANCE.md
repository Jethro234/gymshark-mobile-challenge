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

## 1a. Benchmark harness — read this before the numbers

The payload contains **10 products**. In a two-column grid that is five rows, roughly two
and a half screens — **not enough content to fling**. Frame timing measured over that would
be noise rather than evidence.

Scroll benchmarks therefore run against a `benchmark` build variant that repeats the
committed fixtures to ~500 items, giving a sustained fling to measure.

**This is a measurement harness, not production behaviour.** The shipped app loads 10
products. Startup metrics and the Baseline Profile comparison are unaffected and are honest
figures for the real application; only the scroll figures use the enlarged dataset, and they
are labelled as such in every table below.

---

## 2. Test conditions

| | |
|---|---|
| Device | `TBC` (e.g. Pixel 7, Android 15) |
| Build | `release`, R8 full mode, minified |
| Iterations | 10 |
| Benchmark module | `:macrobenchmark` |
| Metrics | `FrameTimingMetric`, `StartupTimingMetric` |
| Baseline Profile | Generated via `BaselineProfileRule` |

Device state: charged above 80%, airplane mode off but no background sync, screen at fixed
brightness, benchmark run three times with the first discarded to allow thermal settling.

---

## 3. Headline result — Baseline Profile impact

Product grid scroll, 10 iterations.

| Metric | `CompilationMode.None()` | `CompilationMode.Partial()` | Change |
|---|---|---|---|
| `frameOverrunMs` P50 | `TBC` | `TBC` | `TBC` |
| `frameOverrunMs` P90 | `TBC` | `TBC` | `TBC` |
| `frameOverrunMs` P95 | `TBC` | `TBC` | `TBC` |
| `frameOverrunMs` P99 | `TBC` | `TBC` | `TBC` |
| `frameDurationCpuMs` P95 | `TBC` | `TBC` | `TBC` |
| Dropped frames (overrun > 0) | `TBC` | `TBC` | `TBC` |

**Reading this table:** `frameOverrunMs` is how far past its deadline a frame landed.
Positive means the frame was dropped and the user saw a stutter; negative means the frame
finished early by that margin. P95 is the number that matters — P50 being fine while P95 is
positive is exactly what "mostly smooth but noticeably janky" feels like in the hand.

### Startup

| Metric | `None()` | `Partial()` | Change |
|---|---|---|---|
| `timeToInitialDisplayMs` median | `TBC` | `TBC` | `TBC` |
| `timeToFullDisplayMs` median | `TBC` | `TBC` | `TBC` |

Baseline Profiles let ART compile the profiled code paths ahead of time rather than
interpreting and JIT-ing them on first run, typically worth around 30% on code execution
from first launch.

---

## 4. Measure → diagnose → fix → re-measure

This section is the point of the document: not that the app is fast, but that the process
used to make it fast is sound.

### Investigation 1 — `TBC`

**Symptom.** `TBC` — e.g. P95 frame overrun of X ms during grid fling; visible stutter
approximately every N frames.

**Diagnosis.** Opened `traces/before-<fix>.perfetto-trace` in `ui.perfetto.dev` and
inspected the UI thread timeline around the janky frames. `TBC` — what the trace actually
showed, on which thread, in which slice.

**Fix.** `TBC` — the specific change, and why it addresses the cause rather than the symptom.

**Result.**

| Metric | Before | After |
|---|---|---|
| `frameOverrunMs` P95 | `TBC` | `TBC` |
| Dropped frames | `TBC` | `TBC` |

**Traces.** `traces/before-<fix>.perfetto-trace`, `traces/after-<fix>.perfetto-trace`

> To inspect: open <https://ui.perfetto.dev> and drag the file in. Traces are processed
> locally in your browser — nothing is uploaded.

---

## 5. Compose compiler stability report

Enabled with:

```
./gradlew assembleRelease -Pcompose.compiler.reports=true -Pcompose.compiler.metrics=true
```

| Finding | Location | Change made |
|---|---|---|
| `TBC` | `TBC` | `TBC` |

Common classes of finding worth calling out explicitly when they appear: `List<T>`
parameters treated as unstable (fixed with `ImmutableList`), lambdas capturing unstable
receivers, and composables that could not be skipped because a parameter type came from a
module without stability information.

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
