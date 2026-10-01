package io.github.capsicum0907.acervus;

import net.minecraft.network.chat.Component;

public interface Heaped {
    long amount();

    long capacity();

    boolean isEmpty();

    Component contentName();

    String brief(long value);

    String exact(long value);

    default String power(long value) {
        return Counts.power(value);
    }

    int tint();

    default boolean hasKinds() {
        return true;
    }

    default net.minecraft.resources.ResourceLocation contentTexture() {
        return null;
    }

    default int contentTint() {
        return 0xFFFFFFFF;
    }

    default long room() {
        return Math.max(0L, capacity() - amount());
    }

    default boolean gives() {
        return true;
    }

    default boolean unreadable() {
        return false;
    }

    default String unreadableId() {
        return "";
    }

    default boolean locked() {
        return false;
    }

    default boolean canLock() {
        return false;
    }

    default void lock(boolean on) {
    }

    Heaped NONE = new Heaped() {
        @Override
        public long amount() {
            return 0L;
        }

        @Override
        public long capacity() {
            return 0L;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Component contentName() {
            return Component.empty();
        }

        @Override
        public String brief(long value) {
            return Counts.brief(value);
        }

        @Override
        public String exact(long value) {
            return Counts.exact(value);
        }

        @Override
        public int tint() {
            return 0;
        }

        @Override
        public boolean gives() {
            return false;
        }
    };
}
