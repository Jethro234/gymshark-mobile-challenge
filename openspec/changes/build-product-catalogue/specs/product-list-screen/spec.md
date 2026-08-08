## Purpose

Defines the product grid — the app's entry point — including its mutually exclusive states,
what each item shows, how label indicators appear, how refresh and retry behave, and what
survives rotation and process death.

## ADDED Requirements

### Requirement: The screen is in exactly one state at a time

The list screen SHALL be modelled as a closed set of mutually exclusive states: loading,
content, empty and error. It MUST NOT be possible to represent the screen as simultaneously
loading, errored and populated.

#### Scenario: First load shows loading

- **WHEN** the screen is opened and no products have been loaded
- **THEN** the loading state SHALL be shown

#### Scenario: Products load successfully

- **WHEN** the load returns one or more products
- **THEN** the content state SHALL be shown with those products

#### Scenario: Response contains no products

- **WHEN** the load succeeds but returns zero products
- **THEN** the empty state SHALL be shown, distinct from both loading and error

#### Scenario: Load fails

- **WHEN** the load fails
- **THEN** the error state SHALL be shown carrying the typed cause

#### Scenario: Adding a state is a compile-time concern

- **WHEN** a new state is introduced
- **THEN** every place that handles state SHALL be required to handle it
- **AND** an unhandled state MUST NOT result in a blank screen

### Requirement: Error states explain what went wrong in the user's terms

Each typed error cause SHALL produce its own message, so the user can distinguish a
connectivity problem from a service problem. Every error state SHALL offer a retry action.

#### Scenario: No connection

- **WHEN** the error cause is "no connection"
- **THEN** the message SHALL tell the user they are offline

#### Scenario: Server failure

- **WHEN** the error cause is "server"
- **THEN** the message SHALL say the store could not be reached

#### Scenario: Malformed response

- **WHEN** the error cause is "malformed"
- **THEN** a generic "something went wrong" message SHALL be shown

#### Scenario: Retry recovers

- **WHEN** the user activates retry from an error state and the next load succeeds
- **THEN** the content state SHALL be shown

### Requirement: Refresh never blanks the list

The screen SHALL support pull to refresh. While refreshing, the currently displayed products
SHALL remain on screen. A refresh in progress SHALL be represented within the content state,
not as a sibling loading state.

#### Scenario: Refresh in progress

- **WHEN** the user pulls to refresh while products are displayed
- **THEN** the products SHALL remain visible
- **AND** a refresh indicator SHALL be shown

#### Scenario: Refresh succeeds

- **WHEN** a refresh completes successfully
- **THEN** the displayed products SHALL be replaced by the new response
- **AND** the refresh indicator SHALL be dismissed

#### Scenario: Refresh fails with content on screen

- **WHEN** a refresh fails while products are displayed
- **THEN** the products SHALL remain displayed
- **AND** the error state MUST NOT replace them

### Requirement: Each grid item presents its product's data

Every item SHALL show the product's image, title, normalised colourway and price, and SHALL
show a discount treatment when the product is discounted. Titles SHALL clamp to two lines and
colourways to one, without a fixed item height.

#### Scenario: Standard product

- **WHEN** a product with no labels and no compare-at price is displayed
- **THEN** its image, title, colourway and price SHALL be shown with no indicator and no
  strikethrough

#### Scenario: Discounted product

- **WHEN** a product is discounted
- **THEN** the current price SHALL be shown in the sale treatment
- **AND** the compare-at price SHALL be shown struck through

#### Scenario: Long title

- **WHEN** a product title exceeds two lines
- **THEN** it SHALL be truncated at two lines
- **AND** the item SHALL NOT clip its price or colourway

#### Scenario: Long colourway

- **WHEN** a normalised colourway exceeds one line
- **THEN** it SHALL be truncated at one line

### Requirement: Labels are rendered as visible indicators on the item

A product carrying labels SHALL display them as badges inset within the product image. Known
labels SHALL each have a distinct treatment; an unknown label SHALL render with its raw value
title-cased in the quietest treatment, visible but not competing with recognised labels.

#### Scenario: Known label

- **WHEN** a product carries `"going-fast"`
- **THEN** a "Going fast" badge SHALL be displayed inset within the image

#### Scenario: Unknown label

- **WHEN** a product carries a label outside the known set
- **THEN** a badge SHALL be displayed showing the raw value title-cased
- **AND** it SHALL use the outlined, lowest-emphasis treatment

#### Scenario: No labels

- **WHEN** a product's labels are absent or empty
- **THEN** no badge SHALL be displayed and no space SHALL be reserved for one

### Requirement: Selecting a product opens its detail screen

Activating a grid item SHALL navigate to the detail screen for that specific product. Only the
product's identifier SHALL be passed across the navigation boundary.

#### Scenario: Tap opens the correct product

- **WHEN** the user activates a grid item
- **THEN** the detail screen for that product SHALL be shown

#### Scenario: Navigation carries an identifier only

- **WHEN** navigating to the detail screen
- **THEN** only the product identifier SHALL be transferred
- **AND** the full product, its media list and its description MUST NOT be serialised through
  the back stack

### Requirement: Scroll position survives return and process death

The grid's scroll position SHALL be restored when the user returns from the detail screen, and
SHALL survive process death.

#### Scenario: Returning from detail

- **WHEN** the user scrolls the grid, opens a product and presses system back
- **THEN** the grid SHALL be restored to its previous scroll position

#### Scenario: Rotation does not refetch

- **WHEN** the device is rotated while products are displayed
- **THEN** the products SHALL remain displayed
- **AND** no new network request SHALL be issued

### Requirement: The screen draws edge to edge without a bottom bar

The app SHALL draw edge to edge. The grid SHALL apply safe-drawing insets as content padding
so content scrolls beneath the status bar and clears the gesture area.

#### Scenario: Content clears system bars

- **WHEN** the grid is scrolled to its extremes
- **THEN** the first and last items SHALL be fully readable and not obscured by system bars

### Requirement: The screen carries no inert affordances

The list SHALL present only the wordmark, the screen title, the product count and the grid.
Search, menu, sort, filter, favourites, basket and bottom navigation MUST NOT be present.

#### Scenario: No dead controls

- **WHEN** the list screen is displayed
- **THEN** every visible control SHALL perform a real action
- **AND** no affordance implying unimplemented commerce or persistence SHALL be present

#### Scenario: Product count reflects the data

- **WHEN** products are displayed
- **THEN** the count shown SHALL equal the number of products presented
