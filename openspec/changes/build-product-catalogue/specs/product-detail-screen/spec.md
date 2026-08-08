## Purpose

Defines what the user sees after selecting a product — imagery, identity, price, sizes and the
rendered description — and how the screen behaves when it is restored from process death with
an empty cache or a product that no longer exists.

## ADDED Requirements

### Requirement: The detail screen resolves its product from an identifier

The screen SHALL receive only a product identifier and resolve the product itself. It SHALL
model a closed set of states: loading, content and error.

#### Scenario: Product is already cached

- **WHEN** the screen opens for a product present in the cache
- **THEN** the content state SHALL be shown without a network request

#### Scenario: Cache is empty after process death

- **WHEN** the screen is restored after process death and the cache is empty
- **THEN** the loading state SHALL be shown
- **AND** the product SHALL be refetched
- **AND** the content state SHALL follow on success

#### Scenario: Refetch succeeds but the product is gone

- **WHEN** a refetch succeeds and the requested identifier is absent from the response
- **THEN** the error state SHALL be shown with the "not found" cause
- **AND** the message SHALL offer a return to the list
- **AND** the screen MUST NOT crash, spin indefinitely, or present an empty detail layout

#### Scenario: Refetch fails

- **WHEN** the refetch fails
- **THEN** the error state SHALL be shown with the corresponding typed cause and a retry action

### Requirement: The screen presents the product's identity and price

The content state SHALL show the product title, its normalised colourway, its type, and its
price, with the discount treatment applied when the product is discounted.

#### Scenario: Full product information

- **WHEN** a product with all fields present is displayed
- **THEN** the title, colourway, type and price SHALL be shown

#### Scenario: Optional fields absent

- **WHEN** `fit` or `compareAtPrice` is absent
- **THEN** the corresponding element SHALL be omitted entirely rather than shown blank or as a
  placeholder

#### Scenario: Discounted product

- **WHEN** the product is discounted
- **THEN** the sale treatment and struck-through compare-at price SHALL be shown

### Requirement: The screen presents the product's imagery

The content state SHALL show a hero image with any label badge inset, and a thumbnail strip
derived from the product's media. Selecting a thumbnail SHALL change the hero image.

#### Scenario: Hero and thumbnails

- **WHEN** a product with multiple media items is displayed
- **THEN** a hero image SHALL be shown
- **AND** a thumbnail strip SHALL list the product's media

#### Scenario: Selected thumbnail is identifiable

- **WHEN** a thumbnail is selected
- **THEN** it SHALL carry a visible selection outline
- **AND** the hero image SHALL show the corresponding media item

#### Scenario: Product has no media

- **WHEN** the product's media list is empty
- **THEN** no thumbnail strip SHALL be shown
- **AND** the hero SHALL present the image error state

### Requirement: Sizes are shown with their real availability

The screen SHALL present the product's available sizes using the payload's per-size stock
data. Out-of-stock sizes SHALL be shown as unavailable rather than hidden, and SHALL NOT be
selectable.

#### Scenario: In-stock size

- **WHEN** a size is in stock
- **THEN** it SHALL be shown as selectable

#### Scenario: Out-of-stock size

- **WHEN** a size is out of stock
- **THEN** it SHALL be shown in the disabled treatment
- **AND** SHALL NOT respond to selection

#### Scenario: Selecting a size

- **WHEN** the user selects an in-stock size
- **THEN** that size SHALL be shown as selected
- **AND** any previously selected size SHALL be deselected

#### Scenario: Size selection claims nothing further

- **WHEN** a size is selected
- **THEN** no purchase, basket or persistence behaviour SHALL be implied or performed

### Requirement: Selected size survives process death

The selected size is user intent and SHALL be restored after the process is killed and the
screen recreated.

#### Scenario: Selection survives a low-memory kill

- **WHEN** the user selects a size, the process is killed, and the screen is restored
- **THEN** the same size SHALL still be shown as selected

#### Scenario: Selection survives rotation

- **WHEN** the device is rotated after a size is selected
- **THEN** the selection SHALL be retained

### Requirement: Sustainability labels are shown as material chips near the description

A product carrying sustainability labels (see `product-domain-model`) SHALL display each as
a chip positioned after the hairline that follows the size chips, before the description.
This is independent of whether the product also carries a merchandising label — the two
categories render in different places and neither is affected by the other's presence or
absence.

#### Scenario: Product with sustainability labels

- **WHEN** a product carries `"recycled-nylon"` and `"recycled-polyester"`
- **THEN** two material chips SHALL be shown, positioned after the hairline and before the
  description

#### Scenario: Product with no sustainability labels

- **WHEN** a product carries no sustainability labels
- **THEN** no material chip row SHALL be shown and no space SHALL be reserved for one

#### Scenario: Material chips and the merchandising badge are independent

- **WHEN** a product carries both a merchandising label and sustainability labels — the real
  payload contains exactly one such product, carrying `"new"`, `"recycled-nylon"` and
  `"recycled-polyester"`
- **THEN** the hero image badge SHALL show the merchandising label
- **AND** the material chip row SHALL show both sustainability labels
- **AND** neither SHALL be omitted, truncated, or combined into a single overflow indicator

### Requirement: The description is presented as the final content

The content state SHALL end with the rendered description, separated from the sizes by a
hairline. There SHALL be no bottom bar and no call to action.

#### Scenario: Description renders as formatted text

- **WHEN** the content state is displayed
- **THEN** the description heading SHALL appear in the eyebrow style
- **AND** the body SHALL appear as paragraphs and a bullet list

#### Scenario: No commerce affordances

- **WHEN** the detail screen is displayed
- **THEN** no "add to bag", favourite, share or basket control SHALL be present

### Requirement: Back navigation returns to the list

The screen SHALL provide a back affordance, and system back SHALL return to the list screen
with its scroll position intact. Predictive back SHALL behave correctly.

#### Scenario: System back returns to the list

- **WHEN** the user presses system back
- **THEN** the list screen SHALL be shown at its previous scroll position

#### Scenario: Back affordance mirrors in right-to-left layouts

- **WHEN** the layout direction is right to left
- **THEN** the back icon SHALL be mirrored automatically

#### Scenario: The screen's view model is released on pop

- **WHEN** the detail screen is popped from the back stack
- **THEN** its view model SHALL be cleared rather than retained
