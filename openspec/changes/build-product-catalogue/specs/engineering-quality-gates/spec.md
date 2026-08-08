## Purpose

Defines the verifiable standards the deliverable itself must meet — module boundaries, test
strategy, static analysis, commit hygiene, performance measurement method — and the honesty
constraints on every figure and image that reaches the submission. There is no snapshot test
layer: see `design.md` for why (Paparazzi's Gradle plugin is incompatible with the AGP version
Hilt requires).

## ADDED Requirements

### Requirement: Module boundaries are enforced, not decorative

The codebase SHALL be organised into the agreed modules, and the boundaries between them SHALL
be real constraints rather than naming conventions.

#### Scenario: Features do not reach each other

- **WHEN** navigation between screens is required
- **THEN** it SHALL be resolved in the app module
- **AND** no feature module SHALL depend on another feature module

#### Scenario: The domain module stays pure

- **WHEN** the domain module is compiled
- **THEN** it SHALL have no dependency on the Android framework or on Compose

#### Scenario: Fixtures are defined once

- **WHEN** a fixture is needed by both unit tests and instrumented tests
- **THEN** it SHALL be defined once in the shared testing module and reused

### Requirement: Test doubles are hand-written fakes

Test doubles SHALL be hand-written fakes with settable responses and error triggers. A mocking
library MUST NOT be introduced.

#### Scenario: A repository is substituted in a test

- **WHEN** a test needs to control repository behaviour
- **THEN** it SHALL use a hand-written fake
- **AND** SHALL assert on resulting behaviour rather than on recorded interactions

### Requirement: Assertions derive from the requirements, not the implementation

Every assertion SHALL be derived from the brief and the payload. For any non-obvious test, the
implementation SHALL be deliberately broken once to confirm the test fails.

#### Scenario: Writing a test after the code

- **WHEN** a test is written after the unit it covers
- **THEN** its expected values SHALL be derived from the requirement
- **AND** MUST NOT be read off the implementation's actual output

#### Scenario: Confirming a test can fail

- **WHEN** a non-obvious test is added
- **THEN** the implementation SHALL be broken once to observe the failure before the test is
  committed

### Requirement: Fixture data is the real payload

Mapper and sanitiser tests SHALL run against the committed verbatim payload rather than a
tidied approximation. Deliberately corrupted bodies SHALL be declared inline in the tests that
use them, because the corruption is the subject of the assertion.

#### Scenario: Mapper tests use real data

- **WHEN** mapper tests run
- **THEN** they SHALL parse the committed payload resource

#### Scenario: Corrupt bodies are readable in context

- **WHEN** a test asserts behaviour on a malformed or truncated body
- **THEN** that body SHALL be declared within the test rather than in a separate resource file

### Requirement: Static analysis and compilation gates pass without new suppressions

The build SHALL fail on formatting, complexity and lint violations. Suppressions SHALL require
an inline reason, and no baseline file SHALL be used to defer existing violations.

#### Scenario: A unit of work is complete

- **WHEN** a unit of work is declared done
- **THEN** compilation SHALL succeed with warnings treated as errors
- **AND** formatting, static analysis and platform lint SHALL all pass
- **AND** no new suppression SHALL have been added

#### Scenario: No deferral mechanisms

- **WHEN** the lint configuration is inspected
- **THEN** no baseline file SHALL be present

#### Scenario: Prohibited constructs

- **WHEN** production code is reviewed
- **THEN** it SHALL contain no not-null assertion operator, no late-initialised property
  outside test setup, no `TODO` marker and no commented-out code

#### Scenario: User-facing text is externalised

- **WHEN** text is displayed to the user
- **THEN** it SHALL be sourced from string resources
- **AND** MUST NOT be a literal in a composable

### Requirement: Every commit is a working, conventionally described increment

History SHALL be linear with no merge commits. Each commit SHALL compile, SHALL pass its own
tests, and SHALL follow Conventional Commits scoped to the six-module structure.

#### Scenario: Commit message form

- **WHEN** a commit is made
- **THEN** its subject SHALL use an approved prefix, a module scope, imperative mood, and
  fewer than 72 characters
- **AND** its body SHALL explain why when the reason is not obvious

#### Scenario: Scopes match the module structure

- **WHEN** a commit is scoped
- **THEN** the scope SHALL be one of `model`, `data`, `designsystem`, `products` or `app`

#### Scenario: History stays readable

- **WHEN** the log is read
- **THEN** it SHALL contain no work-in-progress, typo-fix or merge commits

### Requirement: Continuous integration reproduces the local verification

A single automated workflow SHALL run on push and pull request, executing formatting, static
analysis, platform lint, JVM unit tests and a release assembly. It SHALL require no secrets and
SHALL be reproducible by anyone who clones the repository.

#### Scenario: Workflow runs without configuration

- **WHEN** the workflow runs on a fresh clone
- **THEN** it SHALL complete without API keys or local configuration

#### Scenario: Device-dependent measurement is excluded

- **WHEN** the workflow is defined
- **THEN** it SHALL NOT run benchmarks, because hosted runners have no physical devices and
  emulator figures are too noisy to gate on

### Requirement: Coverage is reported but not gated

Test coverage SHALL be reported, and the figure for the domain and data modules SHALL be
quoted alongside a plain statement of what is intentionally uncovered. No coverage threshold
SHALL fail the build.

#### Scenario: Coverage is published without a gate

- **WHEN** coverage is reported
- **THEN** no minimum threshold SHALL cause a failure

#### Scenario: Untested areas are named

- **WHEN** coverage is reported
- **THEN** the deliberately untested areas SHALL be listed with their reasons

### Requirement: Performance is designed in and measured on real hardware

Performance-affecting decisions SHALL be taken up front rather than retrofitted, and any
published measurement SHALL come from a physical device running a release build.

#### Scenario: Scrolling work is precomputed

- **WHEN** the grid scrolls
- **THEN** sanitising, currency formatting and label mapping SHALL already have been performed
- **AND** SHALL NOT occur during composition

#### Scenario: List items are reusable

- **WHEN** grid items are declared
- **THEN** each SHALL carry a stable key and a content type

#### Scenario: Presentation models are stable

- **WHEN** a presentation model is declared
- **THEN** it SHALL be marked immutable and SHALL use immutable collection types

#### Scenario: Measurement environment

- **WHEN** any performance figure is published
- **THEN** it SHALL have been produced on a physical device running a minified release build
- **AND** emulator and continuous-integration figures MUST NOT be published

### Requirement: Scroll benchmarking uses a declared measurement harness

Ten products do not produce a sustained scroll. Scroll measurement SHALL run against a
benchmark build variant that repeats the committed fixtures to a dataset large enough to fling,
and every table using it SHALL say so.

#### Scenario: Enlarged dataset is used for scroll metrics

- **WHEN** scroll frame timing is measured
- **THEN** it SHALL run against the enlarged benchmark dataset

#### Scenario: The harness is labelled as such

- **WHEN** scroll figures are published
- **THEN** the accompanying text SHALL state that the dataset is a measurement harness and not
  production behaviour

#### Scenario: Startup figures use the real app

- **WHEN** startup timing and the compilation-mode comparison are measured
- **THEN** they SHALL run against the shipped dataset and SHALL be presented without that caveat

### Requirement: No figure or image in the submission may be invented

Placeholders SHALL remain placeholders until a human replaces them with real output. This
applies to every performance number, every compiler-report finding and every screenshot.

#### Scenario: A performance placeholder is encountered

- **WHEN** a `TBC` value is present in the performance document
- **THEN** it SHALL remain `TBC` until a human runs the benchmark on a physical device
- **AND** it MUST NOT be estimated, extrapolated, or filled from typical or documented values

#### Scenario: Screenshots are required

- **WHEN** the readme references a screenshot
- **THEN** that image SHALL be produced by a human from a committed golden
- **AND** MUST NOT be drawn, mocked up, or sourced by any other means

#### Scenario: The benchmark module exists regardless

- **WHEN** the performance figures have not yet been produced
- **THEN** the benchmark module and baseline profile generation SHALL still be built so that
  the figures can be produced

### Requirement: Ambiguity is escalated rather than resolved by invention

Where a specification is ambiguous, self-contradictory, or cannot be satisfied as written, work
SHALL stop and the question SHALL be raised.

#### Scenario: A specification conflicts with itself

- **WHEN** two documents give conflicting instructions
- **THEN** the documented precedence order SHALL be applied
- **AND** the conflict SHALL be raised rather than silently resolved

#### Scenario: A dependency would be required

- **WHEN** implementation appears to require a dependency outside the approved list
- **THEN** work SHALL stop and approval SHALL be sought

#### Scenario: A test cannot be made to pass

- **WHEN** a test cannot pass without weakening its assertion
- **THEN** the assertion SHALL NOT be weakened and the problem SHALL be raised
