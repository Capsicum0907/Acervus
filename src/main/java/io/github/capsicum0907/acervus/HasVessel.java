package io.github.capsicum0907.acervus;

/**
 * A heap with somewhere to put a container.
 *
 * <p>Separate from {@link Heaped} so that the menu can find the slot without naming
 * any of the three block entities — which matters for the gas one, whose class
 * cannot be mentioned in a game without Mekanism.
 */
public interface HasVessel {
    Vessel vessel(Vessel.Flow flow);
}
