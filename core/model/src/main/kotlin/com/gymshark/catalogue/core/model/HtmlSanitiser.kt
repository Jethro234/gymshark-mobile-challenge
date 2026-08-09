package com.gymshark.catalogue.core.model

/**
 * Cleans the payload's Shopify/TinyMCE-via-Microsoft-Word description HTML into markup
 * [AnnotatedString.fromHtml][androidx.compose.ui.text.AnnotatedString.Companion.fromHtml] can
 * render correctly — see docs/ARCHITECTURE.md §6. No Android or Compose dependency: this is a
 * pure string transformation, testable on a plain JVM.
 */
public fun interface HtmlSanitiser {
    public fun sanitise(raw: String): SanitisedDescription
}

/**
 * @param heading The leading emphasis-only paragraph's text, extracted so it can render with
 *   the `eyebrow` design token instead of an HTML heading tag. `null` if the description
 *   doesn't open with one.
 * @param bodyHtml The remaining description, as markup `fromHtml` understands: `<p>`,
 *   `<strong>`, `<b>`, `<a href>`, `<ul>`/`<li>`. Word and TinyMCE residue removed.
 */
public data class SanitisedDescription(
    public val heading: String?,
    public val bodyHtml: String,
)

/** The production sanitiser. A plain function reference, so it needs no state or DI to test. */
public val DefaultHtmlSanitiser: HtmlSanitiser = HtmlSanitiser(::sanitiseDescription)

internal fun sanitiseDescription(raw: String): SanitisedDescription {
    var html = raw
    html = stripMetaTags(html)
    html = stripDivWrappers(html)
    html = stripMceAttributes(html)
    html = collapseSpanWrappers(html)

    val (heading, afterHeading) = extractLeadingHeading(html)
    val withBullets = convertBulletParagraphs(afterHeading)
    val bodyHtml = collapseWhitespaceAndEmptyParagraphs(withBullets)

    return SanitisedDescription(heading = heading, bodyHtml = bodyHtml)
}

// Every occurrence, not just a leading one — the real payload has a second <meta> tag
// embedded mid-description, inside a bullet item.
private val metaTagPattern = Regex("""<meta[^>]*>""")

internal fun stripMetaTags(html: String): String = html.replace(metaTagPattern, "")

// The real payload contains a Google Translate widget artefact (<div id="gtx-trans">...).
// Its content is decorative chrome, never real text, so unwrapping (not pair-matching, which
// a regex can't do reliably for nested tags) is sufficient and safe.
private val divOpenPattern = Regex("""<div[^>]*>""")

internal fun stripDivWrappers(html: String): String = html.replace(divOpenPattern, "").replace("</div>", "")

// TinyMCE stamps data-mce-fragment (and similar) on almost every tag.
private val mceAttributePattern = Regex("""\s+data-mce-[a-zA-Z0-9-]+="[^"]*"""")

internal fun stripMceAttributes(html: String): String = html.replace(mceAttributePattern, "")

// Word's clipboard signature is span attributes (lang, class="TextRun ...", data-contrast,
// xml:lang, ...) that carry no meaning fromHtml renders — none of these spans in the real
// payload carry an inline `style`, so stripping every span attribute unconditionally is safe.
// A bare <span> is then semantically inert and unwrapped, keeping its text.
private val spanOpenPattern = Regex("""<span[^>]*>""")

internal fun collapseSpanWrappers(html: String): String = html.replace(spanOpenPattern, "").replace("</span>", "")

private val leadingHeadingPattern = Regex("""^\s*<p>\s*<strong>([^<]*)</strong>\s*</p>""")

internal fun extractLeadingHeading(html: String): Pair<String?, String> {
    val match = leadingHeadingPattern.find(html) ?: return null to html
    val heading = match.groupValues[1].trim()
    val remaining = html.removeRange(match.range)
    return heading to remaining
}

private val paragraphPattern = Regex("""<p>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL)
private const val BULLET_PREFIX = "- "

/**
 * A run of `<br>`-delimited lines each beginning with "- " is a bullet list wearing a line
 * break costume (docs/ARCHITECTURE.md §6). Every paragraph is checked independently; only
 * ones where every non-empty segment carries the bullet prefix are converted.
 */
internal fun convertBulletParagraphs(html: String): String =
    paragraphPattern.replace(html) { match ->
        val inner = match.groupValues[1]
        val segments = inner.split("<br>").map(String::trim).filter(String::isNotEmpty)
        val isBulletRun = segments.isNotEmpty() && segments.all { it.startsWith(BULLET_PREFIX) }

        if (isBulletRun) {
            segments.joinToString(separator = "", prefix = "<ul>", postfix = "</ul>") { segment ->
                "<li>${segment.removePrefix(BULLET_PREFIX)}</li>"
            }
        } else {
            match.value
        }
    }

// \s alone does not match U+00A0 (non-breaking space) in Kotlin/Java regex — the payload
// uses the literal character, not the &nbsp; entity, so it must be named explicitly.
private const val WS = """[\s ]"""
private val leadingBrPattern = Regex("""<p>$WS*<br>$WS*""")
private val trailingBrPattern = Regex("""$WS*<br>$WS*</p>""")
private val whitespaceOnlyParagraphPattern = Regex("""<p>$WS*(?:<br>)?$WS*</p>""")

// The source formats one top-level block per line, so a newline separates every <p>. That's
// source formatting, not content — fromHtml gives it no meaning, and left alone it surfaces
// as a stray gap once an empty paragraph between two others is removed.
private val interTagWhitespacePattern = Regex(""">\s*\n\s*<""")

/**
 * Vestigial formatting noise: a leading or trailing `<br>` inside an otherwise-plain
 * paragraph, paragraphs containing nothing but whitespace (including the literal
 * non-breaking-space character the payload uses, not the `&nbsp;` entity), and insignificant
 * newlines between top-level blocks.
 */
internal fun collapseWhitespaceAndEmptyParagraphs(html: String): String =
    html
        .replace(whitespaceOnlyParagraphPattern, "")
        .replace(leadingBrPattern, "<p>")
        .replace(trailingBrPattern, "</p>")
        .replace(interTagWhitespacePattern, "><")
        .trim()
