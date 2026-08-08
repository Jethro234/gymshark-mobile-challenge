## Purpose

Defines the pure domain types the rest of the app reasons about — product identity, money,
labels and colourways — and the observable behaviour each guarantees regardless of where the
data came from or how it is displayed.

## ADDED Requirements

### Requirement: Money is represented in integer minor units

Monetary amounts SHALL be represented as integer minor units and MUST NOT be represented as
`Double`, `Float`, or any other binary floating-point type at any point between parsing and
display. The conversion factor between minor and major units SHALL be a single named constant
so the minor-unit assumption can be reversed in one edit.

#### Scenario: Payload price converts to major units

- **WHEN** a product's `price` field is `1000`
- **THEN** the amount SHALL be interpreted as 1000 minor units
- **AND** SHALL render as `£10.00`

#### Scenario: Small amounts retain precision

- **WHEN** a product's `price` field is `50`
- **THEN** the amount SHALL render as `£0.50`

#### Scenario: Zero is a valid amount

- **WHEN** an amount of `0` minor units is formatted
- **THEN** it SHALL render as `£0.00` and MUST NOT be treated as absent

#### Scenario: The opposite interpretation is covered by test

- **WHEN** the minor-units constant is changed to treat payload values as major units
- **THEN** a unit test SHALL exist that asserts the resulting major-unit rendering
- **AND** no other production code change SHALL be required

### Requirement: Prices are formatted by locale, not by hardcoded symbol

Currency formatting SHALL use the platform's locale-aware currency formatter. No currency
symbol SHALL be hardcoded in a string literal or a composable.

#### Scenario: Currency renders with the locale's conventions

- **WHEN** an amount is formatted for display
- **THEN** the grouping separator, decimal separator and symbol placement SHALL come from the
  active locale
- **AND** the currency SHALL be GBP, because the payload supplies no currency code

### Requirement: Discount presentation follows the merchandiser's figures

A product SHALL be presented as discounted only when `compareAtPrice` is present and strictly
greater than `price`. Any supplied `discountPercentage` SHALL be displayed exactly as
received and MUST NOT be recalculated from the two prices.

#### Scenario: Product is on sale

- **WHEN** `compareAtPrice` is present and greater than `price`
- **THEN** the current price SHALL be marked as discounted
- **AND** the compare-at price SHALL be presented struck through

#### Scenario: Compare-at price is absent

- **WHEN** `compareAtPrice` is `null`
- **THEN** no strikethrough and no discount indicator SHALL be shown

#### Scenario: Compare-at price does not exceed price

- **WHEN** `compareAtPrice` is present but less than or equal to `price`
- **THEN** the product SHALL NOT be presented as discounted

#### Scenario: Supplied discount percentage is trusted

- **WHEN** `discountPercentage` is present
- **THEN** the displayed percentage SHALL equal the supplied value even if it disagrees with
  the arithmetic difference between the two prices

#### Scenario: Discount percentage is absent

- **WHEN** `discountPercentage` is `null`
- **THEN** no percentage SHALL be displayed and none SHALL be computed

### Requirement: Labels are typed with a graceful unknown fallback

Product labels SHALL be parsed into a closed set of known values plus an open `Unknown`
variant carrying the raw string. An unrecognised label MUST NOT crash the app, MUST NOT be
silently dropped, and MUST NOT render as a blank indicator.

#### Scenario: A known label is recognised

- **WHEN** the payload supplies `"going-fast"`
- **THEN** it SHALL parse to the known "going fast" label

#### Scenario: Parsing tolerates case and surrounding whitespace

- **WHEN** the payload supplies `"  GOING-FAST "`
- **THEN** it SHALL parse to the same known label as `"going-fast"`

#### Scenario: An unseen label survives as unknown

- **WHEN** the payload supplies a value not in the known set, such as `"limited-edition"`
- **THEN** it SHALL parse to the unknown variant retaining `"limited-edition"` as its raw value
- **AND** SHALL be available for display

#### Scenario: Null labels yield no indicators

- **WHEN** a product's `labels` field is `null`
- **THEN** the product SHALL have an empty label collection
- **AND** no indicator SHALL be rendered

#### Scenario: Empty label array yields no indicators

- **WHEN** a product's `labels` field is `[]`
- **THEN** the product SHALL have an empty label collection

### Requirement: Multi-value colourways are normalised to one presentation

The payload expresses multiple colours using two different separators. Colour strings SHALL be
normalised by a pure function into a single consistent presentation before display.

#### Scenario: Slash-separated colours

- **WHEN** the colour value is `"Court Blue/Moonstone Blue/White"`
- **THEN** it SHALL render as `Court Blue · Moonstone Blue · White`

#### Scenario: Pipe-separated colours

- **WHEN** the colour value is `"Savanna | Cherry Brown"`
- **THEN** it SHALL render as `Savanna · Cherry Brown`

#### Scenario: Single colour is unchanged

- **WHEN** the colour value contains neither separator
- **THEN** it SHALL render unchanged with no trailing separator

### Requirement: The domain layer carries no Android dependency

Domain types and their supporting pure functions SHALL be expressible and testable on a plain
JVM, with no dependency on the Android framework or on Compose.

#### Scenario: Domain tests run without an Android runtime

- **WHEN** the domain test suite is executed
- **THEN** it SHALL run as plain JVM tests
- **AND** SHALL NOT require an emulator, a device, or a shadowing framework
