## Purpose

Governs the shared visual language — semantic colour, type and spacing tokens — and the
accessibility guarantees every screen inherits from it, including verified contrast, font
scaling, right-to-left support and screen-reader semantics.

## ADDED Requirements

### Requirement: Screens consume tokens, never raw values

All colour, dimension and text style values SHALL be defined once as semantically named tokens
in the design system. Feature and app modules MUST NOT contain a hardcoded colour, dimension
or text style.

#### Scenario: A feature references a colour

- **WHEN** a feature composable needs a colour
- **THEN** it SHALL reference a semantic token such as `textPrimary`
- **AND** MUST NOT reference a literal colour value

#### Scenario: Token names describe role, not appearance

- **WHEN** a token is named
- **THEN** its name SHALL describe its role, such as `surfaceInk`
- **AND** MUST NOT describe its literal appearance, such as `black`

#### Scenario: Spacing stays on the scale

- **WHEN** spacing is applied
- **THEN** it SHALL come from the defined scale of 4, 8, 12, 16, 20, 24 and 32 dp

### Requirement: Light and dark themes are separately authored

Both themes SHALL be complete authored token sets. The dark theme MUST NOT be derived by
inverting the light theme, and dynamic colour MUST NOT be applied.

#### Scenario: Both themes are complete

- **WHEN** the theme is switched
- **THEN** every token SHALL resolve to a value authored for that theme

#### Scenario: Dynamic colour is not used

- **WHEN** the app runs on a device with a user-selected accent colour
- **THEN** the app's palette SHALL be unchanged

### Requirement: Contrast is verified automatically, before the tokens are used

Every foreground and background token pairing SHALL meet WCAG AA: 4.5:1 for normal text, and
3:1 only for large text — at or above 18pt (roughly 24sp), or 14pt bold — and for user
interface boundaries. Compliance SHALL be asserted by an automated test over the token pairs,
not by inspection.

#### Scenario: Contrast test exists before tokens are consumed

- **WHEN** the tokens are introduced
- **THEN** the contrast test SHALL already exist
- **AND** SHALL run as part of the standard test suite

#### Scenario: Body text pairing

- **WHEN** a token pair is used for text below the large-text threshold
- **THEN** its contrast ratio SHALL be at least 4.5:1

#### Scenario: Metadata text is not exempted by being small

- **WHEN** a token is used for small metadata such as the colourway
- **THEN** it SHALL meet the 4.5:1 normal-text threshold
- **AND** the 3:1 large-text allowance MUST NOT be applied to it

#### Scenario: A regression fails the build

- **WHEN** a token value is changed such that a pairing drops below its threshold
- **THEN** the contrast test SHALL fail

### Requirement: All text scales with the user's font settings

Every text size SHALL be expressed in scalable units. No text SHALL be sized in density-
independent pixels. Layouts SHALL reflow rather than clip at large font scales, which requires
that content containers have no fixed heights.

#### Scenario: Font scale is doubled

- **WHEN** the system font scale is set to 2.0
- **THEN** all text SHALL grow proportionally
- **AND** no text SHALL be clipped or overlapped

#### Scenario: Cards size to their content

- **WHEN** a product card's text wraps to more lines than usual
- **THEN** the card SHALL grow to fit rather than truncate below its intended line clamps

### Requirement: Emphasis case is a style, never a transformed string

Uppercase presentation SHALL be applied as a text style. Content strings MUST NOT be
upper-cased in code, because that changes what a screen reader announces and breaks languages
without case.

#### Scenario: Eyebrow text renders uppercase

- **WHEN** eyebrow text is displayed
- **THEN** it SHALL appear uppercase through its text style
- **AND** the underlying string SHALL remain in its authored case

#### Scenario: Content is sentence case

- **WHEN** a product title, colour or control label is displayed
- **THEN** it SHALL be presented in sentence case

### Requirement: The layout supports right-to-left directions

All padding, alignment and positioning SHALL use start and end rather than left and right.
Directional icons SHALL use automatically mirroring variants.

#### Scenario: Layout mirrors

- **WHEN** the layout direction is right to left
- **THEN** all horizontal padding, alignment and inset positions SHALL mirror

#### Scenario: Directional icons mirror

- **WHEN** the layout direction is right to left
- **THEN** the back icon SHALL point in the mirrored direction

#### Scenario: Mirroring is pinned by a snapshot

- **WHEN** the snapshot suite runs
- **THEN** a committed golden SHALL capture the list screen in right-to-left layout

### Requirement: Interactive elements meet minimum target size

Every interactive element SHALL have a touch target of at least 48dp in both dimensions,
regardless of its drawn size.

#### Scenario: Small controls have adequate targets

- **WHEN** a size chip or a back affordance is drawn smaller than 48dp
- **THEN** its touch target SHALL still be at least 48dp

### Requirement: State and meaning are conveyed to assistive technology

Information conveyed visually SHALL also be available non-visually. Colour and decoration
alone MUST NOT be the sole carrier of meaning.

#### Scenario: Unavailable size

- **WHEN** a size is out of stock and drawn with a strikethrough
- **THEN** its state SHALL also be exposed as a state description of "out of stock"

#### Scenario: Label indicators

- **WHEN** a label badge is displayed
- **THEN** its meaning SHALL be available as text, not conveyed by fill colour alone

#### Scenario: Reading order matches visual order

- **WHEN** a screen is traversed with a screen reader
- **THEN** the traversal order SHALL match the visual reading order

### Requirement: Motion is limited to what serves comprehension

The app SHALL use a short crossfade when images load, and SHALL support predictive back. No
other motion SHALL be added — in particular no staggered list entrance animation and no
shared-element transition.

#### Scenario: Image load

- **WHEN** an image resolves
- **THEN** it SHALL crossfade in over approximately 200ms

#### Scenario: No entrance animation on scroll

- **WHEN** grid items enter the viewport
- **THEN** they SHALL appear without a staggered or animated entrance

### Requirement: Every element on screen is backed by data or behaviour

The interface SHALL contain no element that neither displays payload data nor performs a real
action.

#### Scenario: Auditing the surface

- **WHEN** any screen is reviewed
- **THEN** every visible element SHALL either display data from the payload or perform an
  implemented action
- **AND** no placeholder, sample or inert control SHALL be present
