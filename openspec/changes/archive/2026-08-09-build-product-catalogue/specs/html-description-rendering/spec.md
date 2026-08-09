## Purpose

Turns the payload's hostile Shopify/TinyMCE-via-Microsoft-Word description HTML into styled,
themed, accessible on-screen text — a heading, paragraphs and a genuine bullet list — without
a WebView. This is the challenge's headline graded requirement.

## ADDED Requirements

### Requirement: Description HTML is never shown as markup

The product description SHALL be rendered as formatted text. Raw tags, attributes or entity
references MUST NOT appear on screen under any circumstance, including when sanitising
encounters content it does not recognise.

#### Scenario: Markup does not reach the screen

- **WHEN** the detail screen renders the committed payload's description
- **THEN** no `<p>`, `<span>`, `<strong>`, `<br>`, `data-mce-fragment` or `<meta>` text SHALL
  be visible

### Requirement: The description is rendered without a WebView

Rendering SHALL use native text composition. A `WebView` MUST NOT be used, and an embedded
Android view hosting HTML MUST NOT be used.

#### Scenario: No web view in the detail screen

- **WHEN** the detail screen is composed
- **THEN** the description SHALL be drawn by native text composition
- **AND** SHALL inherit the app's theme, colour tokens and the user's font scale

### Requirement: Sanitising is a pure, independently testable transformation

The transformation from raw payload HTML to clean, renderable content SHALL be a pure function
with no Android or Compose dependency, testable on a plain JVM. It SHALL return the extracted
heading separately from the remaining body HTML.

#### Scenario: Sanitising runs as a plain JVM test

- **WHEN** the sanitiser test suite runs
- **THEN** it SHALL execute without an Android runtime

#### Scenario: The real description is a golden regression test

- **WHEN** the exact description string from the committed payload is sanitised
- **THEN** the output SHALL equal a committed expected value
- **AND** any future change to that output SHALL fail the test

#### Scenario: Sanitising is written before the implementation

- **WHEN** the sanitiser is built
- **THEN** its expected output SHALL be authored first and the implementation written to satisfy it

### Requirement: Word and TinyMCE artefacts are removed

Sanitising SHALL remove the upstream editor's residue as ordered, individually tested steps.

#### Scenario: Inline meta tag is stripped

- **WHEN** the raw description contains `<meta charset="utf-8">` inside the body
- **THEN** it SHALL be removed

#### Scenario: Editor attributes are stripped

- **WHEN** elements carry `data-mce-*` attributes
- **THEN** those attributes SHALL be removed while the element's semantics are preserved

#### Scenario: Word clipboard classes are stripped

- **WHEN** an element carries a class such as `TextRun SCXP103297068 BCX0`
- **THEN** the class attribute SHALL be removed

#### Scenario: Semantically empty wrappers are collapsed

- **WHEN** a `<span>` carries no remaining semantic meaning after attribute stripping
- **THEN** the wrapper SHALL be collapsed and its text content retained

#### Scenario: Empty paragraphs and redundant whitespace are collapsed

- **WHEN** sanitising leaves empty paragraphs or runs of whitespace
- **THEN** they SHALL be collapsed so no blank gaps appear on screen

### Requirement: The leading emphasis paragraph becomes a heading

A leading paragraph whose entire content is emphasised SHALL be extracted as a separate
heading value rather than left inline, and SHALL be presented using the design system's
eyebrow text style.

#### Scenario: Heading is extracted

- **WHEN** the description begins with a paragraph containing only `<strong>RUN WITH IT</strong>`
- **THEN** `RUN WITH IT` SHALL be returned as the heading
- **AND** SHALL NOT also appear in the body content

#### Scenario: Heading uses design tokens, not HTML heading tags

- **WHEN** the heading is rendered
- **THEN** it SHALL use the eyebrow token's size, weight and letter spacing
- **AND** MUST NOT be produced by emitting `<h1>`–`<h6>`, whose platform size multipliers would
  override the type scale

#### Scenario: No leading emphasis paragraph exists

- **WHEN** the description does not begin with an emphasis-only paragraph
- **THEN** no heading SHALL be extracted and the body SHALL render unchanged

### Requirement: Line-break-delimited runs become a real bullet list

A run of `<br>`-delimited lines each beginning with a hyphen is a bullet list wearing a line
break costume. Sanitising SHALL convert such a run into proper list markup, and rendering
SHALL draw actual bullets with hanging indentation.

#### Scenario: Hyphenated run converts to list markup

- **WHEN** the body contains `- Full length legging<br>- High-waisted<br>- Sweat wicking`
- **THEN** sanitising SHALL emit an unordered list of three items
- **AND** the leading hyphen and space SHALL NOT remain in the item text

#### Scenario: Bullets are drawn on screen

- **WHEN** the sanitised list is rendered
- **THEN** each item SHALL display a bullet glyph
- **AND** wrapped lines SHALL indent under the item text rather than under the bullet

#### Scenario: Bullets are not faked with text characters

- **WHEN** bullets fail to render
- **THEN** the failure SHALL be raised as a version-floor problem
- **AND** MUST NOT be worked around by emitting bullet characters as body text

### Requirement: The rendering library floor is pinned and verified

Bullet rendering depends on a minimum Compose `ui-text` version. That floor SHALL be pinned in
the version catalog, and bullet rendering SHALL be verified by running the detail screen on a
device or emulator before the screen is considered complete. No automated test layer in this
project can exercise `AnnotatedString.fromHtml` — it requires an Android runtime, which is
unavailable to the JVM unit suite, and there is no snapshot or Robolectric layer to bridge the
gap (both cut; see `design.md`).

#### Scenario: Version floor is enforced

- **WHEN** the Compose dependency set is resolved
- **THEN** `ui-text` SHALL be at version 1.9 or later

#### Scenario: Bullet rendering is confirmed by manual verification

- **WHEN** the detail screen is run on a device or emulator against the real sanitised
  description
- **THEN** the rendered output SHALL show actual bullets with hanging indentation
- **AND** this check SHALL be performed before the detail screen is considered complete, not
  deferred to final review

### Requirement: Conversion to styled text runs once per description

The presentation model SHALL carry the heading and body as plain strings. Conversion of body
HTML to styled text SHALL be memoised against the body string so it runs once per description
rather than on every frame.

#### Scenario: Presentation state holds plain strings

- **WHEN** the detail presentation model is constructed
- **THEN** it SHALL expose the heading and body as strings
- **AND** its tests SHALL run as plain JVM tests without an Android runtime

#### Scenario: Conversion is memoised

- **WHEN** the detail screen recomposes without the description changing
- **THEN** the HTML-to-styled-text conversion SHALL NOT be repeated

### Requirement: Unrecognised markup degrades safely

The renderer silently discards tags it does not understand, so sanitising is a correctness
requirement rather than a courtesy. Content that survives sanitising SHALL be restricted to
markup the renderer supports.

#### Scenario: Unsupported markup does not silently delete content

- **WHEN** the raw description contains markup the renderer cannot represent
- **THEN** sanitising SHALL either convert it to supported markup or reduce it to plain text
- **AND** the underlying words SHALL remain visible to the user
