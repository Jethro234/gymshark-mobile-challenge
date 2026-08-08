package com.gymshark.catalogue.core.model

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * An amount of money, stored as integer minor units so arithmetic and comparison are exact.
 * Never backed by [Double] or [Float] — see docs/ARCHITECTURE.md §7.
 */
@JvmInline
public value class Money(
    public val minorUnits: Long,
) : Comparable<Money> {
    override fun compareTo(other: Money): Int = minorUnits.compareTo(other.minorUnits)

    /**
     * Formats this amount as GBP using [locale]'s grouping, decimal separator and symbol
     * placement. The currency itself is always GBP — the payload supplies no currency code.
     */
    public fun format(locale: Locale = Locale.getDefault()): String {
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = Currency.getInstance(CURRENCY_CODE)
        return formatter.format(BigDecimal.valueOf(minorUnits, MINOR_UNIT_SCALE))
    }

    public companion object {
        /** Single named constant: reversing the major/minor interpretation is a one-line edit. */
        public const val MINOR_UNITS_PER_MAJOR: Long = 100L

        private const val MINOR_UNIT_SCALE: Int = 2
        private const val CURRENCY_CODE: String = "GBP"

        /**
         * The payload's `price` and `compareAtPrice` fields are major units (pounds), not
         * minor units — see docs/ARCHITECTURE.md §7. This is the mapper's entry point.
         */
        public fun fromMajorUnits(majorUnits: Long): Money = Money(majorUnits * MINOR_UNITS_PER_MAJOR)

        /** The reversed interpretation, kept so the assumption in [fromMajorUnits] is a one-line swap. */
        public fun fromMinorUnits(minorUnits: Long): Money = Money(minorUnits)

        public val ZERO: Money = Money(0L)
    }
}
