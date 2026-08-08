## Purpose

Governs how the supplied Algolia payload is obtained, parsed, mapped, cached and refreshed,
and how every failure mode is classified into a typed cause the user interface can speak
about in plain language.

## ADDED Requirements

### Requirement: The real payload is committed before any mapping work

The response from the supplied endpoint SHALL be fetched once and committed verbatim to the
shared testing module's resources before any mapper or fixture work begins. It MUST NOT be
hand-written, trimmed, reformatted or otherwise tidied.

#### Scenario: Payload is committed unaltered

- **WHEN** the payload is committed as a test resource
- **THEN** it SHALL be byte-identical to the endpoint response
- **AND** SHALL retain the `null` labels, the Word-mangled description HTML and both colour
  separators

#### Scenario: Mapping work is blocked without it

- **WHEN** the committed payload resource is absent
- **THEN** mapper implementation SHALL NOT begin
- **AND** the absence SHALL be raised rather than worked around with an invented fixture

### Requirement: Parsing tolerates unknown and missing fields

Deserialisation SHALL ignore payload fields the app does not model, so that an upstream
addition cannot crash the app. Every field the payload marks as nullable SHALL be modelled as
optional and SHALL have a defined behaviour when absent.

#### Scenario: Unmodelled fields are ignored

- **WHEN** the Algolia envelope contains fields the app does not model
- **THEN** parsing SHALL succeed and the unmodelled fields SHALL be discarded

#### Scenario: Nullable product fields are absent

- **WHEN** `fit`, `compareAtPrice`, `discountPercentage`, `alt` or `labels` is `null`
- **THEN** mapping SHALL succeed
- **AND** the corresponding presentation SHALL be omitted rather than rendered blank

#### Scenario: Featured media is missing

- **WHEN** a product has no `featuredMedia`
- **THEN** mapping SHALL succeed and the product SHALL be presented with no primary image
  source, resolving to the image error state

#### Scenario: Media array is empty

- **WHEN** a product's `media` array is empty
- **THEN** mapping SHALL succeed and no thumbnail strip SHALL be presented

### Requirement: The product list is presented flat

The ten payload hits are five distinct products in ten colourways. The list SHALL present
every hit as its own item, in the order the endpoint returns them. Hits MUST NOT be grouped,
deduplicated or reordered by title, handle or colour.

#### Scenario: All hits appear as separate items

- **WHEN** the committed payload of ten hits is mapped
- **THEN** ten separate products SHALL be presented
- **AND** the three `Speed Leggings` colourways SHALL each appear as their own item

### Requirement: Products are served from an in-memory cache after first load

The repository SHALL fetch the payload once and serve subsequent reads from an in-memory cache
whose lifetime exceeds that of any single screen, so the detail screen can read a product the
list screen loaded.

#### Scenario: Second read avoids a network call

- **WHEN** products are requested a second time and the cache is populated
- **THEN** the cached list SHALL be returned
- **AND** no additional network request SHALL be issued

#### Scenario: Detail screen reads what the list loaded

- **WHEN** a product is requested by id and that id is present in the cache
- **THEN** the cached product SHALL be returned without a network request

### Requirement: Refresh bypasses the cache and never destroys content

An explicit refresh SHALL force a network fetch and replace the cached list. If that fetch
fails, the previously loaded content SHALL be retained and the failure SHALL be surfaced
transiently. A populated list MUST NOT be replaced by an error screen.

#### Scenario: Refresh fetches from the network

- **WHEN** refresh is invoked while the cache is populated
- **THEN** a network request SHALL be issued
- **AND** a successful response SHALL replace the cached list

#### Scenario: Refresh fails while content is on screen

- **WHEN** refresh fails and content was previously loaded
- **THEN** the existing content SHALL remain presented
- **AND** the failure SHALL be communicated without clearing the list

### Requirement: Failures are mapped to typed causes, not thrown types

Every failure reaching the presentation layer SHALL be expressed as one of a closed set of
causes: no connection, server, malformed, not found, or unknown. A raw `Throwable` MUST NOT
appear in presentation state.

#### Scenario: Connectivity failure

- **WHEN** the request fails because the device has no network connection
- **THEN** the cause SHALL be "no connection"

#### Scenario: Server rejects the request

- **WHEN** the endpoint responds with HTTP 500
- **THEN** the cause SHALL be "server"

#### Scenario: Request times out

- **WHEN** the socket times out
- **THEN** the cause SHALL be "no connection"

#### Scenario: Body cannot be parsed

- **WHEN** the response body is malformed JSON
- **THEN** the cause SHALL be "malformed"

#### Scenario: Body is truncated mid-stream

- **WHEN** the response body ends before the payload is complete
- **THEN** the cause SHALL be "malformed"

#### Scenario: Response is valid but carries no products

- **WHEN** the endpoint returns `{"hits": []}`
- **THEN** the result SHALL be an empty product list and SHALL NOT be classified as an error

#### Scenario: Cause is unclassifiable

- **WHEN** a failure matches none of the above
- **THEN** the cause SHALL be "unknown"

### Requirement: Failure behaviour is proved without relying on the endpoint

The supplied endpoint is a static CDN file that cannot fail. Every failure path above SHALL be
exercised against a local mock server, asserted end to end from repository through presentation
state. Failure handling MUST NOT be asserted by claiming behaviour the real endpoint cannot
demonstrate.

#### Scenario: Each failure mode has an end-to-end test

- **WHEN** the test suite runs
- **THEN** HTTP 500, socket timeout, malformed body, truncated body and empty hits SHALL each
  be served by a mock server
- **AND** each SHALL be asserted to produce the correct typed presentation state

### Requirement: Threading is injected, never hardcoded

No component SHALL reference a concrete dispatcher directly. The dispatcher used for I/O SHALL
be supplied by injection so tests can substitute a deterministic one.

#### Scenario: Tests control execution deterministically

- **WHEN** a repository or view model is placed under test
- **THEN** its dispatcher SHALL be replaceable without modifying production code
