package io.github.capsicum0907.acervus;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;

public final class Counts {
    public static final String GROUP_KEY = "acervus.count.group";
    public static final String UNITS_KEY = "acervus.count.units";
    public static final String GROUP = "3";
    public static final String UNITS = "K M G T P E";
    public static final String INFINITE = "∞";

    private static Language read;
    private static Notation notation;

    private Counts() {
    }

    public record Notation(long base, List<Long> sizes, List<String> suffixes) {
        public static Notation of(int digits, String units) {
            long base = 1L;
            for (int i = 0; i < digits; i++) {
                base *= 10L;
            }
            List<Long> sizes = new ArrayList<>();
            List<String> suffixes = new ArrayList<>();
            long size = base;
            for (String suffix : units.trim().split("\\s+")) {
                sizes.add(size);
                suffixes.add(suffix);
                if (size > Long.MAX_VALUE / base) {
                    break;
                }
                size *= base;
            }
            return new Notation(base, List.copyOf(sizes), List.copyOf(suffixes));
        }
    }

    public static Notation notation() {
        Language language = Language.getInstance();
        if (language != read || notation == null) {
            int digits;
            try {
                digits = Integer.parseInt(language.getOrDefault(GROUP_KEY, GROUP).trim());
            } catch (NumberFormatException e) {
                digits = Integer.parseInt(GROUP);
            }
            notation = Notation.of(digits, language.getOrDefault(UNITS_KEY, UNITS));
            read = language;
        }
        return notation;
    }

    public static long stored(CompoundTag tag, String key) {
        return stored(tag, key, key);
    }

    public static long stored(CompoundTag tag, String key, String legacy) {
        Tag value = tag.contains(key) ? tag.get(key) : tag.get(legacy);
        if (!(value instanceof NumericTag number)) {
            return 0L;
        }
        boolean fraction = value instanceof DoubleTag || value instanceof FloatTag;
        return Math.max(0L, fraction ? (long) number.getAsDouble() : number.getAsLong());
    }

    public static int inAnInt(long count, boolean roomLeft) {
        if (count < Integer.MAX_VALUE) {
            return (int) Math.max(0L, count);
        }
        return roomLeft ? Integer.MAX_VALUE - 1 : Integer.MAX_VALUE;
    }

    public static long beyondAnInt(long count, boolean roomLeft) {
        return count - inAnInt(count, roomLeft);
    }

    public static long beyondAnInt(long count) {
        return Math.max(0L, count - Integer.MAX_VALUE);
    }

    public static String exact(long count) {
        return String.format("%,d", count);
    }

    public static String brief(long count) {
        return brief(count, notation());
    }

    public static String brief(long count, Notation notation) {
        List<Long> sizes = notation.sizes();
        if (sizes.isEmpty() || count < sizes.get(0)) {
            return Long.toString(count);
        }
        int unit = 0;
        while (unit + 1 < sizes.size() && count >= sizes.get(unit + 1)) {
            unit++;
        }
        long tenths = count / (sizes.get(unit) / 10L);
        return mantissa(tenths) + notation.suffixes().get(unit);
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

    private static String mantissa(long tenths) {
        return tenths % 10 == 0 ? Long.toString(tenths / 10) : (tenths / 10) + "." + (tenths % 10);
    }
}
