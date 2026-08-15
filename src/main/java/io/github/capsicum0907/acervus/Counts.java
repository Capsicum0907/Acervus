package io.github.capsicum0907.acervus;

/**
 * How a number of items is written down.
 *
 * <p>Two forms, and which one is used says what the reader is doing. Glancing at a
 * block across a room, or at a screen while sorting, wants a size — <em>about two
 * billion</em> — and thirteen digits is not a size, it is a wall. Asking for detail
 * wants the number itself. So the short form is what is drawn, and the exact form
 * is what appears when somebody asks.
 *
 * <p>Both live here rather than at the two places that draw them, because a block
 * and its screen disagreeing about the same contents is the kind of thing nobody
 * notices and everybody distrusts.
 */
public final class Counts {
    /** Below the smallest unit the number is already three digits, which is the target. */
    private static final long SMALLEST_UNIT = 1_000L;

    /**
     * SI, largest first, and the whole range of a long is covered by six single
     * letters — {@code Long.MAX_VALUE} is about 9.2E. The English short scale would
     * read more naturally at the low end, but it runs out at T and continues into
     * spellings nobody knows; these are also what AE2 puts on stored item counts, so
     * anyone who has used one has already learnt them.
     */
    private static final long[] UNITS = {
            1_000_000_000_000_000_000L, 1_000_000_000_000_000L, 1_000_000_000_000L,
            1_000_000_000L, 1_000_000L, 1_000L };
    private static final String[] SUFFIXES = { "E", "P", "T", "G", "M", "K" };

    private Counts() {
    }

    /** Every digit, grouped. For tooltips and anywhere the reader asked. */
    public static String exact(long count) {
        return String.format("%,d", count);
    }

    /**
     * At most three digits, at most one decimal, and a unit: {@code 100M},
     * {@code 2.1G}, {@code 999.9T}.
     *
     * <p>Three digits is the point of it: a number that fits in a glance. It is also
     * what makes grouping unnecessary here — there is never a fourth digit to
     * separate — while {@link #exact} keeps its commas.
     */
    public static String brief(long count) {
        if (count < SMALLEST_UNIT) {
            return Long.toString(count);
        }
        for (int unit = 0; unit < UNITS.length; unit++) {
            if (count < UNITS[unit]) {
                continue;
            }
            // Rounding can push 999.97G up to 1000.0G, which is four digits and the
            // wrong unit. When it does, the number has grown into the next one.
            if (tenths((double) count / UNITS[unit]) >= 10_000L && unit > 0) {
                unit--;
            }
            return mantissa((double) count / UNITS[unit]) + SUFFIXES[unit];
        }
        return Long.toString(count); // unreachable: anything at or above a unit found one
    }

    private static long tenths(double value) {
        return Math.round(value * 10.0);
    }

    /** One decimal, and no decimal at all when it would be a nought. */
    private static String mantissa(double value) {
        long tenths = tenths(value);
        return tenths % 10 == 0 ? Long.toString(tenths / 10) : (tenths / 10) + "." + (tenths % 10);
    }
}
