# Conventions

Build setup, git strategy and code style. The parts of a repository that are read
unconsciously and judged immediately.

---

## 1. Git

> They asked for "a publicly accessible git repository". The first thing a lead does is
> read the log. It is free evidence of how work is decomposed, and it cannot be
> retrofitted.

### Strategy

**Linear history. No merge commits. Conventional Commits. Every commit compiles and passes
its tests.**

Roughly 25–35 commits, ordered so the log reads as the build story:

```
chore: initialise project with version catalog and convention plugins
chore: configure detekt, ktlint and android lint
feat(model): add Product, Money and Label domain types
test(model): cover Money minor-unit conversion and formatting
test(model): cover Label parsing including unknown fallback
feat(model): add HtmlSanitiser                      ← written test-first
test(model): cover HtmlSanitiser against real payload description
feat(data): add Algolia DTOs and Retrofit service
feat(data): map DTOs to domain, handling null labels and multi-value colours
test(data): cover mapper against committed payload fixture
feat(data): add ProductRepository with in-memory cache and refresh bypass
test(data): cover error mapping for server, timeout and malformed responses
test(designsystem): assert WCAG AA contrast across all token pairs
feat(designsystem): add colour, type and spacing tokens for light and dark
feat(designsystem): add GsAsyncImage with loading and error states
feat(designsystem): add GsLabelBadge including unknown treatment
feat(products): add ProductListViewModel and UiState
test(products): cover loading, content, empty and error transitions
feat(products): add product grid screen
test(products): add Paparazzi goldens for tier 1 states
feat(products): add detail screen with sanitised description and bullet list
test(products): pin bullet rendering with a Paparazzi golden
feat(products): restore selected size across process death
feat(products): handle refetch returning no matching product id
feat(app): wire Navigation 3 with type-safe routes
test(app): add navigation and retry instrumented tests
perf: generate baseline profile
perf: memoise fromHtml conversion with remember
docs: add architecture, design and performance documentation
```

Scopes match the six-module structure: `model` · `data` · `designsystem` · `products` ·
`app`. There is no `network`, `productlist` or `productdetail` scope.

### Rules

- **Prefixes:** `feat` · `fix` · `test` · `refactor` · `perf` · `docs` · `chore`, scoped by
  module.
- **Subject line under 72 characters, imperative mood.** "add", not "added" or "adding".
- **Body explains why, never what.** The diff already says what.
- **No `wip`, no `fix typo`, no `.` commits.** Rebase them away before pushing.
- **No commented-out code, no `TODO` left in a final commit.** If it's worth doing it's an
  issue; if it isn't, delete it.
- `main` is the only branch. Solo work with self-reviewed pull requests reads as
  performative; a clean linear history reads as competent.

### `.gitignore`

Standard Android ignores, plus `local.properties`, `.idea/` except code style files, and
Macrobenchmark output. **Paparazzi goldens are committed** — they are the test expectations,
not build output.

---

## 2. Build setup

### Version catalog

All dependencies and versions in `gradle/libs.versions.toml`. No hardcoded version strings
in any module. Bundles for things that always travel together (`compose`, `testing`,
`paparazzi`).

### Convention plugins

Eight modules with hand-maintained build files is duplication waiting to drift. A
`build-logic` included build supplies:

| Plugin | Applies to |
|---|---|
| `gymshark.android.library` | compileSdk/minSdk, Kotlin options, common test deps |
| `gymshark.android.library.compose` | Compose compiler, BOM, preview tooling |
| `gymshark.android.feature` | The above plus Hilt, ViewModel, navigation |
| `gymshark.jvm.library` | Pure Kotlin modules (`:core:model`) |
| `gymshark.android.test` | Paparazzi, Robolectric, fixture wiring |

Result: a feature module's build file is about six lines. That reduction *is* the argument
for the structure.

### SDK levels

- `compileSdk` / `targetSdk`: latest stable.
- `minSdk` **26**. Covers effectively the entire active device base, and avoids desugaring
  workarounds for APIs used here. Stated in the README rather than left for the reader to
  infer from a Gradle file.

### Annotation processing — KSP, never KAPT

Hilt and any other processors run through **KSP**. KAPT works by generating Java stubs for
every Kotlin source file before processing, which is the dominant cost in most Android
builds; KSP reads Kotlin symbols directly and is typically around twice as fast on a
Hilt-heavy project. It is also the only processor path with a future — KAPT is in
maintenance and reads as dated in a 2026 codebase.

```kotlin
plugins { alias(libs.plugins.ksp) }
dependencies { ksp(libs.hilt.compiler) }
```

### Compiler configuration

- **Explicit API mode** on `:core:*` modules — public declarations must state visibility and
  return types. Cheap discipline on a shared surface.
- `allWarningsAsErrors` on CI builds.
- Compose compiler metrics behind a Gradle property (see `PERFORMANCE.md` §5).
- R8 full mode on release, with the Baseline Profile applied.

---

## 3. Static analysis

Run in that order, all failing the build:

1. **ktlint** — formatting. Never argued about, because it isn't configurable enough to
   argue about.
2. **detekt** — complexity, long methods, magic numbers, unused code. Config committed;
   suppressions require an inline reason, never a blanket baseline.
3. **Android Lint** — `warningsAsErrors`, `abortOnError`. **No baseline file.** A baseline is
   a promise to fix things later, and on a new repository there is no "later" to defer to.

---

## 4. Code style

- **No `!!` anywhere.** If nullability is genuinely impossible, express that in the type.
- **No `lateinit`** outside test setup.
- **Public API of `:core:*` is documented with KDoc**; private implementation is not — the
  code should carry that itself.
- **Comments explain why, never what.** The one place comments earn their place here is
  `HtmlSanitiser`, where each step needs its upstream quirk named, because the reason is not
  inferable from the code.
- **All user-facing text in `strings.xml`.** Not for translation — for the accessibility
  and testing story, and because hardcoded strings in Composables are the single most common
  thing a reviewer greps for.
- **No hardcoded dimensions or colours in feature modules.** Tokens only.
- Test names in backticks, stating behaviour:
  `` `maps product with null labels to empty label list`() ``.
  Not `testMapper2`.

---

## 5. What CI runs

A single GitHub Actions workflow on push and pull request:

```
ktlint → detekt → lint → unit tests (JVM) → Paparazzi verify → Robolectric → assembleRelease
```

Fast, free, reproducible by anyone who clones the repository, and no secrets required.

**Macrobenchmark is deliberately absent** — GitHub Actions has no physical devices and
emulator figures are too noisy to gate on. Performance numbers come from local runs on real
hardware and are published in `PERFORMANCE.md`, which says so plainly.
