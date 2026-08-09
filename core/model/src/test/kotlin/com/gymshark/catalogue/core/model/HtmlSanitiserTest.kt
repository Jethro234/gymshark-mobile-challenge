package com.gymshark.catalogue.core.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HtmlSanitiserTest {
    // ---- Step: strip <meta> tags (task 3.3) ----

    @Test
    fun `strips a leading meta tag`() {
        val input = """<meta charset="utf-8"><p>Body</p>"""

        assertEquals("<p>Body</p>", stripMetaTags(input))
    }

    @Test
    fun `strips a meta tag embedded mid-document, not just a leading one`() {
        // The real payload's first description has a second <meta> inside a bullet item.
        val input = """<p>Model is <meta charset="utf-8">5'3"</p>"""

        assertEquals("<p>Model is 5'3\"</p>", stripMetaTags(input))
    }

    // ---- Step: strip stray <div> wrappers (task 3.3) ----

    @Test
    fun `strips a google translate widget div, a real artefact in the payload`() {
        // Verbatim shape from the "Adapt Animal Seamless Leggings" description.
        val input = """<p>Text</p>
<div id="gtx-trans" style="position: absolute; left: 44px; top: 382.5px;">
<div class="gtx-trans-icon"></div>
</div>"""

        assertEquals("<p>Text</p>\n\n\n", stripDivWrappers(input))
    }

    // ---- Step: strip data-mce-* attributes (task 3.3) ----

    @Test
    fun `strips data-mce-fragment attributes while preserving the tag`() {
        val input = """<p data-mce-fragment="1"><strong data-mce-fragment="1">Text</strong></p>"""

        assertEquals("<p><strong>Text</strong></p>", stripMceAttributes(input))
    }

    @Test
    fun `does not strip unrelated attributes such as href`() {
        val input = """<a data-mce-fragment="1" href="https://example.com">link</a>"""

        assertEquals("""<a href="https://example.com">link</a>""", stripMceAttributes(input))
    }

    // ---- Step: collapse Word clipboard <span> wrappers (task 3.3) ----

    @Test
    fun `strips word clipboard span attributes and unwraps the now-bare span`() {
        // Verbatim shape from the real payload: nested spans around "5'3" and wears a size M".
        val input =
            """<span lang="EN-GB" class="TextRun SCXP103297068 BCX0" data-contrast="none" """ +
                """data-usefontface="true" xml:lang="EN-GB"><span class="NormalTextRun SCXP103297068 BCX0">""" +
                """5'3" and wears a size M</span></span>"""

        assertEquals("""5'3" and wears a size M""", collapseSpanWrappers(input))
    }

    @Test
    fun `a span with no attributes is also unwrapped`() {
        assertEquals("Text", collapseSpanWrappers("<span>Text</span>"))
    }

    // ---- Step: extract a leading emphasis-only paragraph as the heading (task 3.4) ----

    @Test
    fun `extracts a leading strong-only paragraph as the heading`() {
        val input = "<p><strong>RUN WITH IT</strong></p><p>Body text.</p>"

        val (heading, remaining) = extractLeadingHeading(input)

        assertEquals("RUN WITH IT", heading)
        assertEquals("<p>Body text.</p>", remaining)
    }

    @Test
    fun `no heading is extracted when the description does not open with an emphasis-only paragraph`() {
        val input = "<p>Just a normal opening paragraph.</p>"

        val (heading, remaining) = extractLeadingHeading(input)

        assertNull(heading)
        assertEquals(input, remaining)
    }

    @Test
    fun `a paragraph mixing strong with other text is not treated as a heading`() {
        val input = "<p><strong>Bold</strong> and more text.</p>"

        val (heading, remaining) = extractLeadingHeading(input)

        assertNull(heading)
        assertEquals(input, remaining)
    }

    // ---- Step: convert <br>-delimited "- " runs into <ul><li> (task 3.5) ----

    @Test
    fun `converts a br-delimited hyphen run into a real list, removing the hyphen and space`() {
        val input = "<p>- Full length legging<br>- High-waisted<br>- Compressive fit</p>"

        val expected = "<ul><li>Full length legging</li><li>High-waisted</li><li>Compressive fit</li></ul>"
        assertEquals(expected, convertBulletParagraphs(input))
    }

    @Test
    fun `a leading br before the first bullet does not prevent conversion`() {
        // The real payload's bullet paragraphs open with a stray <br> before the first "- ".
        val input = "<p><br>- First<br>- Second</p>"

        assertEquals("<ul><li>First</li><li>Second</li></ul>", convertBulletParagraphs(input))
    }

    @Test
    fun `a paragraph that is prose, not a bullet run, is left as a plain paragraph`() {
        val input = "<p><br>Your run requires enduring comfort and support.</p>"

        assertEquals(input, convertBulletParagraphs(input))
    }

    @Test
    fun `a paragraph is only converted when every segment carries the bullet prefix`() {
        val input = "<p>- Real bullet<br>Not a bullet</p>"

        assertEquals(input, convertBulletParagraphs(input))
    }

    // ---- Step: collapse whitespace and empty paragraphs (task 3.6) ----

    @Test
    fun `collapses a whitespace-only paragraph containing the literal non-breaking space`() {
        // The real payload uses the literal U+00A0 character, not the &nbsp; entity.
        val input = "<p>Real content.</p><p> <br></p>"

        assertEquals("<p>Real content.</p>", collapseWhitespaceAndEmptyParagraphs(input))
    }

    @Test
    fun `collapses an entirely empty paragraph`() {
        val input = "<p>Real content.</p><p></p>"

        assertEquals("<p>Real content.</p>", collapseWhitespaceAndEmptyParagraphs(input))
    }

    @Test
    fun `strips a leading br from an otherwise-plain paragraph`() {
        val input = "<p><br>Text follows.</p>"

        assertEquals("<p>Text follows.</p>", collapseWhitespaceAndEmptyParagraphs(input))
    }

    @Test
    fun `strips a trailing br from an otherwise-plain paragraph`() {
        val input = "<p>Find out more here.<br></p>"

        assertEquals("<p>Find out more here.</p>", collapseWhitespaceAndEmptyParagraphs(input))
    }

    // ---- Golden regression: the real committed description (task 3.1, 3.7) ----

    @Test
    fun `sanitises the real Speed Leggings Navy description exactly`() {
        val raw = readResource("speed-leggings-navy-description.html")

        val result = DefaultHtmlSanitiser.sanitise(raw)

        assertEquals("RUN WITH IT", result.heading)
        assertEquals(EXPECTED_SPEED_LEGGINGS_BODY, result.bodyHtml)
    }

    private fun readResource(name: String): String =
        checkNotNull(javaClass.classLoader.getResource(name)) { "Missing test resource: $name" }
            .readText()

    private companion object {
        // Authored by hand before the implementation existed — the test-first exception
        // documented in docs/ARCHITECTURE.md §9.0. Traced step by step against the raw
        // string in speed-leggings-navy-description.html:
        //  - the leading <meta> and the leading <strong>-only paragraph are gone (heading)
        //  - the second, mid-bullet <meta> is gone
        //  - the nested Word spans around "5'3" and wears a size M" are unwrapped to plain text
        //  - the whitespace-only middle paragraph (nbsp + br) is gone entirely
        //  - the leading <br> in the "Your run requires..." paragraph is gone
        //  - the ten "- "-prefixed, <br>-delimited segments become a real <ul><li> list
        private const val EXPECTED_SPEED_LEGGINGS_BODY =
            "<p>Your run requires enduring comfort and " +
                "support, so step out and hit the road in Speed. Made with zero-distractions and " +
                "lightweight, ventilating fabrics that move with you, you can trust in Speed no " +
                "matter how far you go.</p><ul>" +
                "<li>Full length legging</li>" +
                "<li>High-waisted</li>" +
                "<li>Compressive fit</li>" +
                "<li>Internal adjustable elastic/drawcord at front waistband</li>" +
                "<li>Pocket to back of waistband</li>" +
                "<li>Reflective Gymshark sharkhead logo to ankle</li>" +
                "<li>Main: 88% Polyester 12% Elastane. Internal Mesh: 76% Nylon 24% Elastane</li>" +
                "<li>We've cut down our use of swing tags, so this product comes without one</li>" +
                "<li>Model is\u00A05'3\" and wears a size M</li>" +
                "<li>SKU:\u00A0B3A3E-UBCY</li>" +
                "</ul>"
    }
}
