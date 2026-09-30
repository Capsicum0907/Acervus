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

    /** A thousand millibuckets to the bucket, which is the only reason this exists. */
    private static final long PER_BUCKET = 1_000L;

    /**
     * A quantity of fluid, said in buckets.
     *
     * <p>Millibuckets are what the game's plumbing counts in, and they cost three
     * digits of every number for nothing: the same int that counts two billion items
     * counts two million buckets. The unit is not ours to change, but which unit is
     * <em>shown</em> is, and a player counts buckets.
     *
     * <p>Below a bucket there is nothing to round to, so those are said as they are.
     */
    public static String buckets(long millibuckets) {
        if (millibuckets < PER_BUCKET) {
            return millibuckets + " mB";
        }
        return brief(millibuckets / PER_BUCKET) + " B";
    }

    /** Every millibucket of it, for when the detail was asked for. */
    public static String exactBuckets(long millibuckets) {
        return exact(millibuckets / PER_BUCKET) + " B " + (millibuckets % PER_BUCKET) + " mB";
    }

    private static final String SUPERSCRIPT_DIGITS = "⁰¹²³⁴⁵⁶⁷⁸⁹";

    public static String power(long value) {
        int exponent = exponentOf(value);
        return exponent < 0 ? exact(value) : "10" + superscript(exponent);
    }

    public static String powerBuckets(long millibuckets) {
        if (millibuckets >= PER_BUCKET && millibuckets % PER_BUCKET == 0L
                && exponentOf(millibuckets / PER_BUCKET) >= 0) {
            return power(millibuckets / PER_BUCKET) + " B";
        }
        return exponentOf(millibuckets) >= 0 ? power(millibuckets) + " mB" : exactBuckets(millibuckets);
    }

    private static int exponentOf(long value) {
        if (value <= 0L) {
            return -1;
        }
        int exponent = 0;
        while (value % 10L == 0L) {
            value /= 10L;
            exponent++;
        }
        return value == 1L ? exponent : -1;
    }

    private static String superscript(int exponent) {
        StringBuilder out = new StringBuilder();
        for (char digit : Integer.toString(exponent).toCharArray()) {
            out.append(SUPERSCRIPT_DIGITS.charAt(digit - '0'));
        }
        return out.toString();
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
