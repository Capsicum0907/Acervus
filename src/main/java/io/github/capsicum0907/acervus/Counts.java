package io.github.capsicum0907.acervus;

public final class Counts {
    private static final long SMALLEST_UNIT = 1_000L;

    private static final long[] UNITS = {
            1_000_000_000_000_000_000L, 1_000_000_000_000_000L, 1_000_000_000_000L,
            1_000_000_000L, 1_000_000L, 1_000L };
    private static final String[] SUFFIXES = { "E", "P", "T", "G", "M", "K" };

    private Counts() {
    }

    public static long beyondAnInt(long count) {
        return Math.max(0L, count - Integer.MAX_VALUE);
    }

    public static String exact(long count) {
        return String.format("%,d", count);
    }

    public static String brief(long count) {
        if (count < SMALLEST_UNIT) {
            return Long.toString(count);
        }
        for (int unit = 0; unit < UNITS.length; unit++) {
            if (count < UNITS[unit]) {
                continue;
            }
            if (tenths((double) count / UNITS[unit]) >= 10_000L && unit > 0) {
                unit--;
            }
            return mantissa((double) count / UNITS[unit]) + SUFFIXES[unit];
        }
        return Long.toString(count);
    }

    private static final long PER_BUCKET = 1_000L;

    public static String buckets(long millibuckets) {
        if (millibuckets < PER_BUCKET) {
            return millibuckets + " mB";
        }
        return brief(millibuckets / PER_BUCKET) + " B";
    }

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

    private static String mantissa(double value) {
        long tenths = tenths(value);
        return tenths % 10 == 0 ? Long.toString(tenths / 10) : (tenths / 10) + "." + (tenths % 10);
    }
}
