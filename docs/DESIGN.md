# Design specification

**Direction:** luxury-retail *structure* — generous gutters, large corner radii, pill
controls, calm typographic hierarchy — rendered in Gymshark's monochrome palette with a
sans face. The composure of an editorial fashion app without adopting a visual language
that belongs to a different brand.

**Base is light, not dark.** gymshark.com and the Gymshark app are predominantly white with
black type and black CTAs — the black in the brand is the ink and the buttons, not the
canvas. Monochrome-on-white is therefore both the airier structure we want and closer to
what the brand actually ships.

All tokens live in `:core:designsystem`. No screen ever references a raw colour, dimension
or text style.

---

## 1. Colour tokens

Semantic names, never literal ones — `surfaceBrand`, not `black`. Both themes are authored
sets; dark mode is not an inversion of light.

| Token | Light | Dark | Use |
|---|---|---|---|
| `background` | `#FFFFFF` | `#0B0B0B` | Page canvas |
| `surface` | `#F5F5F3` | `#1F1F1D` | Image placeholder, chips, skeletons |
| `surfaceInk` | `#0B0B0B` | `#FFFFFF` | Nav bar, primary CTA, badge fill |
| `onSurfaceInk` | `#FFFFFF` | `#0B0B0B` | Text on `surfaceInk` |
| `textPrimary` | `#0B0B0B` | `#FFFFFF` | Titles, prices |
| `textSecondary` | `#5F5F5B` | `#A5A5A2` | Body copy, description |
| `textMuted` | `#656561` | `#8A8A84` | Colourway, metadata, eyebrow labels |
| `textDisabled` | `#B4B4AE` | `#4A4A48` | Out-of-stock size chips |
| `border` | `#E6E6E2` | `#2A2A28` | Hairlines, unselected chips |
| `borderStrong` | `#C8C8C2` | `#4A4A48` | Selected-adjacent chips |
| `sale` | `#A32D2D` | `#F09595` | Discounted price only |
| `success` | `#0F6E56` on `#E1F5EE` | `#9FE1CB` on `#0F3D31` | Sustainability material chips (`GsMaterialChip`) |

`sale` is the only chromatic colour in the product surface. Restraint is the point: one
accent means the discount actually reads as an exception.

### Contrast

Every text pairing meets **WCAG AA**: 4.5:1 for normal text, 3:1 only for large text
(≥18pt / ~24sp, or 14pt bold). `border` and `borderStrong` are decorative separators —
hairlines and card edges, not indicators a user needs to operate the interface — so they are
not required-for-function boundaries under WCAG 1.4.11 and are exempt from the 3:1
UI-boundary allowance that phrase would otherwise invoke. Both measure well under 3:1 against
`background` by design: the subtlety is the point ("separation comes from whitespace and
hairlines," §3).

`textMuted` was previously `#8C8C88` / `#77776F`, which measure **3.38:1** and **4.36:1** on
`background` — both failing. The earlier justification ("used only at 11sp+") also inverted
the rule: the 3:1 allowance is for *large* text, and 11sp is small text needing 4.5:1.
`textMuted` carries the colourway on every card, which is product information, not
decoration.

First corrected to `#767672` / `#8A8A84`, verified only against `background`. That value
still failed against `surface` (**4.18:1** — `GsAsyncImage`'s error caption sits on a
`surface` fill), the other real place `textMuted` is used. Corrected again to `#656561`
(light only; the dark value already cleared both), which clears both contexts with margin:
**5.85:1** on `background`, **5.36:1** on `surface`. Dark stays `#8A8A84` — **5.67:1** on
`background`, **4.76:1** on `surface`.

`textDisabled` (`#B4B4AE` / `#4A4A48`) is the one text-carrying token deliberately excluded
from the 4.5:1 assertion: on `background` it measures **2.08:1** (light) and **2.22:1**
(dark), both well short. It is used for exactly one thing — out-of-stock size-chip labels —
which are inactive controls, exempt under WCAG 1.4.3's carve-out for text that is part of a
disabled component. Darkening it to pass would make it read as available, defeating the
control's purpose; the non-visual signal is carried separately by the chip's
`stateDescription` of "out of stock" (§4), not by this token's contrast.

Verified by a unit test over every token pair actually used together, not by eye — including
the `surface` pairing the first correction missed. **That test is written before the tokens
are used**, so a regression here fails the build rather than shipping.

---

## 2. Typography

Single sans family, two weights. Named styles map onto Material 3's type scale so M3
components inherit correctly.

| Style | Size / line height | Weight | Letter spacing | Use |
|---|---|---|---|---|
| `displayScreen` | 27 / 29 | 500 | −0.01em | Screen title ("Womens leggings") |
| `titleProduct` | 22 / 25 | 500 | −0.005em | Detail screen product name |
| `titleCard` | 13 / 17 | 500 | 0 | Product name in grid |
| `price` | 18 / 22 | 500 | 0 | Detail price |
| `priceCard` | 12 / 16 | 500 | 0 | Grid price |
| `body` | 12 / 20 | 400 | 0 | Description copy |
| `label` | 11 / 15 | 400 | 0 | Colourway, chips, badges |
| `eyebrow` | 11 / 15 | 500 | 0.13em | "SIZE", "10 PRODUCTS" |

**`eyebrow` is the only uppercase style in the app**, and it is applied as a *style*, never
by upper-casing the string. Uppercasing content breaks screen readers (they spell out short
tokens) and breaks languages without case. Product titles, colours and button labels are
sentence case.

**All sizes are `sp` and scale with user font settings.** No `dp` text anywhere. Cards are
intrinsically sized — no fixed heights — so a 2.0 font scale reflows rather than clips. This
is not independently verified — the snapshot layer that would have pinned it is absent, not
reduced (see §8 below and `design.md` §8 of the `build-product-catalogue` change) — so the
claim rests on the layout never fixing a height, not on a recorded check.

---

## 3. Spacing, shape, elevation

- **Spacing scale:** 4 / 8 / 12 / 16 / 20 / 24 / 32 dp. Nothing off-scale.
- **Screen gutter:** 20dp. Grid gutter: 14dp. This generosity is the direction — resist
  tightening it to fit more product.
- **Corner radii:** `card` 20dp · `hero` 22dp · `thumbnail` 10dp · `chipPill` 999dp ·
  `sheet` 26dp.
- **Elevation: none.** No shadows anywhere. Separation comes from whitespace and hairlines.
  Flat surfaces are what make the layout read as considered rather than as stacked cards.

---

## 4. Components

### `GsProductCard`

Intrinsic height. Image (`card` radius, aspect ratio reserved from the payload's own
`featuredMedia.width`/`height` — ~4:5 across the real payload, close to but not exactly the
3:4 sketched here; a fixed ratio was rejected because it would crop or letterbox against the
real photos) → 11dp → title → 3dp → colourway → 7dp → price. No border, no background, no
shadow — the photograph is the card. Title clamps to 2 lines, colourway to 1.

Multi-value colours are normalised: the payload's `"Court Blue/Moonstone Blue/White"` and
`"Savanna | Cherry Brown"` both render `·`-separated. Pure function, unit-tested.

### `GsAsyncImage`

Wraps Coil 3 `AsyncImage` (not `SubcomposeAsyncImage` — see `ARCHITECTURE.md` §12).

Three states share **one shape instance**:

- **Loading** — `surface` fill, subtle shimmer.
- **Loaded** — the image, `ContentScale.Crop`.
- **Error** — `surface` fill, 0.5dp `border`, `ti-photo-off` equivalent icon in `textDisabled`,
  caption "Image unavailable" in `textMuted`.

**The radius must be identical across all three.** A mismatch produces a visible corner pop
on every image that loads during a scroll — the exact class of defect the brief's "error
free user experience" is testing for.

Aspect ratio is reserved from the payload's `width`/`height` before load, so the grid never
reflows mid-scroll.

`contentDescription` = `alt` if present, else the product title. `alt` is `null` on every
product in this payload, so the fallback is the real path, not a defensive one.

### `GsLabelBadge`

Merchandising labels only (`ARCHITECTURE.md` §8) — one per product, at most, since none of
the sixty products in the real payload carries more than one. Pill, `label` style, sentence
case, 4dp × 11dp padding. Positioned 10dp inset from the image's top-left, inside the
rounded corner.

Two visual tiers, not four distinct looks — restraint over decoration, the same principle
that keeps `sale` the surface's only chromatic colour. Meaning is carried by text; the tier
is carried by weight:

| Label | Tier | Treatment |
|---|---|---|
| Going fast | Urgency | `surfaceInk` fill, `onSurfaceInk` text |
| Limited edition | Urgency | `surfaceInk` fill, `onSurfaceInk` text |
| New | Informational | `surface` fill, `textPrimary` text |
| Popular | Informational | `surface` fill, `textPrimary` text |
| `Unknown(raw)` | — | Transparent fill, 0.5dp `borderStrong`, `textSecondary` text, raw value title-cased |

The `Unknown` treatment is deliberately the quietest of all three: an unrecognised label
from the CMS should be visible without competing with labels we understand, and stay
visually distinct from both known tiers rather than borrowing either one's weight.

`Back in stock` and `Sold out` were part of an earlier, invented label vocabulary and do not
appear in the real payload; there is no treatment for them.

### `GsMaterialChip`

Sustainability labels (`recycled-nylon`, `recycled-polyester`) — genuine material-provenance
data, not urgency or novelty, so it does not compete for the image badge slot. Rendered as
one chip per sustainability label in a row on the detail screen, immediately before the
description (see §5). Pill, `label` style, sentence case, `success` fill/text pair — green
reading as the conventional retail signal for sustainable material, and a token pair already
contrast-verified with nothing else to use it now that `success`'s original "Back in stock"
badge doesn't exist in the real label vocabulary.

A product can show zero, one or two material chips independently of whether it also shows a
merchandising badge — the one product carrying `new` plus both recycled labels shows the
`New` badge on its image and both material chips on its detail screen, with no stacking,
overflow or "+2" affordance anywhere.

### `GsSizeChip`

Pill. Selected: `surfaceInk` fill. Available: `borderStrong` outline, `textPrimary`.
Out of stock: `border` outline, `textDisabled`, strikethrough — **and a
`stateDescription` of "out of stock"**, because strikethrough alone conveys nothing to a
screen reader.

### Window insets

There is no bottom bar (see *Deliberately absent*), but the app still draws edge to edge —
`enableEdgeToEdge()`, with the grid applying `WindowInsets.safeDrawing` as `contentPadding`
so content scrolls under the status bar and clears the gesture bar. Getting insets right
without a `Scaffold` bar to do it for you is the more careful path, not the lazier one.

### Deliberately absent

**Scope rule: nothing ships that doesn't do something.** The brief specifies a product list
and a product detail screen. Affordances implying commerce or persistence that isn't built
are cut rather than faked:

- **No "Add to bag"** — there is no basket. A dead primary button is worse than no button,
  and a reviewer taps it before they read any README caveat.
- **No favourite / heart** — no persistence layer, and adding one purely to light up an
  icon is scope invented to justify decoration.
- **No bottom navigation bar** — the brief specifies two screens. A four-tab retail shell
  with three inert destinations is the largest piece of dead UI available to build.

The detail screen therefore ends with the description. This is a deliberate choice, stated
in the README, not an omission.

**Size chips are the exception, and they earn it:** `availableSizes` carries genuine
per-size `inStock` and `inventoryQuantity`. Rendering them — including out-of-stock sizes as
disabled — displays data the payload actually contains, which is *product information*, the
thing the brief asks the detail screen to show. Selection is local UI state and claims
nothing further.

### Wordmark

Text lockup: `GYMSHARK` in the app's own sans at 13sp, weight 500, 0.2em tracking. No image
asset — it keeps the mark inside the type system and avoids putting a registered trademark
into a public repository.

---

## 5. Screens

### Product list

`GYMSHARK` wordmark, centred → 20dp → `displayScreen` title → 10dp → `eyebrow` count →
18dp → 2-column `LazyVerticalGrid`, 14dp gutters, running to the bottom of the screen.

The top bar carries the wordmark alone — the menu and search icons are cut for the same
reason as everything else in *Deliberately absent*.

Grid items carry a stable `key` (product id) and `contentType`. Pull-to-refresh is
supported and **never blanks the list** — `isRefreshing` lives inside `Content`.

### Product detail

Back → hero image (aspect ratio reserved from the selected media's own dimensions, per
`product-imagery`) with badge → 10dp → thumbnail strip (selected thumbnail carries a
1.5dp `textPrimary` outline) → 18dp → title → colourway · type → price → `SIZE` eyebrow →
size chips → hairline → material chips (if any) → sanitised description. No bottom bar, no
CTA.

The description is the visible proof of the HTML work: `<strong>RUN WITH IT</strong>`
renders as a heading, and the `<br>`-delimited run renders as a real bullet list. The README
shows the raw source string beside the rendered result.

`Add to bag` is **cut** — settled in §9. There is no basket in scope, and a dead primary
button is worse than no button.

---

## 6. Motion

- Image crossfade on load: 200ms.
- Predictive back, which Navigation 3 supports natively.

**Shared-element transition is cut.** It was never in `SCOPE.md`'s build order or its 20
hours, and it conflicts with `ARCHITECTURE.md` §11.2: after process death the detail screen
opens in `Loading` with no image to land on, so the transition would need a fallback path
that costs more than the effect is worth here.
- Nothing else. No staggered list entrance animations — they look impressive in a demo and
  cost frames on every scroll, which conflicts with the performance goals.

---

## 7. Accessibility

- Every interactive target ≥ 48dp, including size chips and the back affordance.
- All text in `sp`; layouts left intrinsically sized so a 2.0 font scale reflows. Not
  independently verified — see §7's note on the missing snapshot layer above.
- Images carry a real `contentDescription`; decorative elements are explicitly cleared.
- Badges expose their meaning as text, not colour alone.
- Out-of-stock sizes convey state via `stateDescription`, not strikethrough alone.
- Touch order and semantic order match reading order.
- **RTL supported.** All padding and alignment use `start`/`end`, never `left`/`right`;
  directional icons (the detail screen back arrow) use `AutoMirrored` variants. Verified
  with a one-off manual on-device check — `adb shell settings put global force_rtl 1`
  (needs a full `adb reboot` to actually propagate) — not a golden, since the snapshot
  layer doesn't exist in this project. The check found a real bug: `android:supportsRtl`
  was missing from `AndroidManifest.xml` entirely, so the app silently rendered LTR-only
  despite every modifier already correctly using `start`/`end`. Fixed, then reverified —
  full mirroring confirmed on both screens including the `AutoMirrored` back icon.
- Verified with a TalkBack pass on device, documented in the README.

---

## 8. No snapshot layer — what replaced it

This section originally planned a Paparazzi golden matrix (`ProductListScreen`'s seven
states, `GsProductCard`'s seven variants, `GsLabelBadge`'s five treatments,
`ProductDetailScreen`'s four states, `GsSizeChip`'s three states, each in light/dark, the
card set additionally at font scale 2.0, `ProductListScreen` once more in RTL — roughly 60
goldens against a 1.5h budget, already earmarked for a tiered reduction before hour 18).

**None of it was built.** Paparazzi's Gradle plugin is incompatible with the AGP version
Hilt's Gradle plugin (2.60.1) hard-requires; Hilt is used throughout the app while Paparazzi
is one test layer, so the pre-authorised fallback in `SCOPE.md` §6 applies — the layer is
dropped, not reduced. Full reasoning: `design.md` §8 of the `build-product-catalogue`
openspec change. Concretely:

- **The two states a golden would have proved — bullet rendering and RTL mirroring — are
  one-off manual on-device checks instead**, performed before their respective screens were
  considered complete. Both are recorded above (§7) and in `ARCHITECTURE.md` §9.3. Both
  checks found real bugs a golden would also have caught: the aspect-ratio hardcode
  (`GsProductCard` originally used a fixed 3:4 ratio instead of the payload's own
  `width`/`height`, contradicting the `product-imagery` spec) and the missing
  `android:supportsRtl` manifest flag.
- **The image error state's visibility is unaffected** — it was always proved by a fixture
  with a dead URL, visible on first launch, not by a golden.
- **Everything else this matrix would have covered — light/dark theming, label badge
  treatments, font-scale reflow beyond the manual RTL/bullet checks, size chip states — has
  no remaining automated or manual coverage in this project.** The unit suite covers the
  *behaviour* behind each state (which `UiState` a given input produces, which label maps to
  which treatment); it does not verify what any of it looks like on screen. That gap is
  named here rather than left for a reviewer to discover.

---

## 9. Settled

| Decision | Outcome |
|---|---|
| `Add to bag` | **Cut** — no basket exists |
| Favourite / heart | **Cut** — no persistence exists |
| Size chips | **Kept** — genuine payload data, selection is local state only |
| Wordmark | **Text lockup**, no image asset |
| Bottom navigation | **Cut** — two screens in scope |
| Menu / search icons | **Cut** — no drawer, no search |

**The governing rule, for the README:** nothing ships that doesn't do something. Every
element on screen is backed by data from the payload or by real behaviour. The brief asks
for a product list with images and label indicators, a detail screen, and the HTML
description presented appropriately — the app is exactly that, executed completely, rather
than a larger surface with inert parts.
