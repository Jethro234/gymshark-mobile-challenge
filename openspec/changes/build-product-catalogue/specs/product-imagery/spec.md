## Purpose

Covers how product photography is loaded and what the user sees while it loads, when it
succeeds, and when it is missing or broken — the challenge's explicit "handle incorrect
and/or missing images" requirement.

## ADDED Requirements

### Requirement: Every image has three distinct visible states

An image SHALL present a loading state, a loaded state and an error state, each visually
distinct. The error state SHALL be composed from design tokens — a surface fill, a hairline
border, an icon and a caption — and MUST NOT be a plain grey box or a static bundled drawable.

#### Scenario: Image is loading

- **WHEN** an image request is in flight
- **THEN** a surface-filled placeholder with a subtle shimmer SHALL occupy the image's bounds

#### Scenario: Image loads successfully

- **WHEN** the image request succeeds
- **THEN** the image SHALL be drawn cropped to fill its bounds

#### Scenario: Image fails to load

- **WHEN** the image request fails for any reason
- **THEN** a composed error state SHALL be shown containing an icon and a caption reading
  "Image unavailable"
- **AND** the surrounding layout SHALL NOT shift

#### Scenario: Product has no image source at all

- **WHEN** a product carries no usable image URL
- **THEN** the error state SHALL be shown rather than an empty gap

### Requirement: All three states share one shape

The corner radius of the loading, loaded and error states SHALL be identical, so no corner
change is visible at the moment an image resolves.

#### Scenario: Radius does not change on load

- **WHEN** an image transitions from loading to loaded during a scroll
- **THEN** the corner radius SHALL be unchanged
- **AND** no corner pop SHALL be visible

### Requirement: Layout is reserved before the image arrives

The space an image will occupy SHALL be reserved from the dimensions supplied in the payload
before the image loads, so content never reflows mid-scroll.

#### Scenario: Aspect ratio comes from payload dimensions

- **WHEN** the payload supplies `width` and `height` for a media item
- **THEN** those values SHALL determine the reserved aspect ratio before the request completes

#### Scenario: Grid does not jump as images resolve

- **WHEN** images resolve while the grid is being scrolled
- **THEN** no item SHALL change size and no re-measure pass SHALL be triggered by the load

### Requirement: Images are decoded at display size

Image requests SHALL carry explicit size hints so a full-resolution source is never decoded at
its native size into a smaller destination.

#### Scenario: Large source in a grid cell

- **WHEN** a source image substantially larger than its destination is requested
- **THEN** it SHALL be decoded at approximately the destination size

### Requirement: Images carry meaningful accessibility descriptions

Every product image SHALL expose a content description. The payload's `alt` field SHALL be used
when present; because it is `null` on every product in the supplied payload, the product title
SHALL be the operative fallback.

#### Scenario: Alt text is supplied

- **WHEN** a media item has a non-empty `alt` value
- **THEN** that value SHALL be the image's content description

#### Scenario: Alt text is absent

- **WHEN** `alt` is `null` or blank
- **THEN** the product title SHALL be the image's content description

#### Scenario: Decorative imagery is excluded from semantics

- **WHEN** an image conveys no information beyond decoration
- **THEN** its semantics SHALL be explicitly cleared rather than given an empty description

### Requirement: Image loading is not subcomposed

Image presentation SHALL avoid subcomposition, which is measurably more expensive within a
scrolling grid. State SHALL be observed through the loader's own state reporting instead.

#### Scenario: Grid items avoid subcomposition

- **WHEN** a grid item renders an image
- **THEN** its loading and error states SHALL be resolved without subcomposing per item

### Requirement: The error state is visible without reading tests

One product in the committed fixture data SHALL point at a dead image URL so the error state
appears on first launch of the app.

#### Scenario: Reviewer sees the error state immediately

- **WHEN** the app is launched against the committed fixture data
- **THEN** at least one grid item SHALL display the composed image error state
- **AND** the remaining items SHALL display their images normally

### Requirement: Loading and error states are deterministically reproducible

The image component SHALL expose a seam that lets a test force each of the three states
without performing a network request, so every state can be captured as a snapshot.

#### Scenario: Snapshot tests force each state

- **WHEN** the snapshot suite renders the image component
- **THEN** the loading state and the error state SHALL each be reproducible deterministically
- **AND** no network access SHALL be required
