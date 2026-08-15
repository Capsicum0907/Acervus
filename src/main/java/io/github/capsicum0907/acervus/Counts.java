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
    /** Below this, the number is short enough to be its own summary. */
    private static final long EXACT_BELOW = 10_000L;

    private static final long[] UNITS = { 1_000_000_000_000L, 1_000_000_000L, 1_000_000L, 1_000L };
    private static final String[] SUFFIXES = { "T", "B", "M", "K" };

    private Counts() {
    }

    /** Every digit, grouped. For tooltips and anywhere the reader asked. */
    public static String exact(long count) {
        return String.format("%,d", count);
    }

    /** Three significant figures and a suffix. For anything drawn at a glance. */
    public static String brief(long count) {
        if (count < EXACT_BELOW) {
            return exact(count);
        }
        for (int i = 0; i < UNITS.length; i++) {
            if (count >= UNITS[i]) {
                double scaled = (double) count / UNITS[i];
                String number = scaled < 10.0 ? String.format("%.2f", scaled)
                        : scaled < 100.0 ? String.format("%.1f", scaled)
                        : String.format("%.0f", scaled);
                return number + SUFFIXES[i];
            }
        }
        return exact(count); // unreachable: anything at or above the threshold has a unit
    }
}
