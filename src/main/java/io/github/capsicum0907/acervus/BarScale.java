package io.github.capsicum0907.acervus;

public enum BarScale {
    LINEAR,
    LOG,
    DECADE;

    public BarScale next() {
        BarScale[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public double fraction(long amount, long capacity) {
        if (amount <= 0L || capacity <= 0L) {
            return 0.0;
        }
        if (amount >= capacity) {
            return 1.0;
        }
        return switch (this) {
            case LINEAR -> (double) amount / (double) capacity;
            case LOG -> Math.log10(amount) / Math.log10(capacity);
            case DECADE -> {
                long lower = decadeFloor(amount);
                long upper = decadeCeiling(amount, capacity);
                yield upper <= lower ? 1.0 : (double) (amount - lower) / (double) (upper - lower);
            }
        };
    }

    public double[] ticks(long capacity) {
        if (this != LOG || capacity <= 10L) {
            return new double[0];
        }
        double decades = Math.log10(capacity);
        int count = (int) Math.ceil(decades) - 1;
        double[] ticks = new double[count];
        for (int tick = 0; tick < count; tick++) {
            ticks[tick] = (tick + 1) / decades;
        }
        return ticks;
    }

    public static long decadeFloor(long amount) {
        long lower = 1L;
        while (lower <= amount / 10L) {
            lower *= 10L;
        }
        return lower;
    }

    public static long decadeCeiling(long amount, long capacity) {
        long lower = decadeFloor(amount);
        return lower > capacity / 10L ? capacity : Math.min(capacity, lower * 10L);
    }
}
