# Acervus

One kind of item, in numbers a chest cannot hold.

*Acervus* is Latin for a heap.

> **Status: scaffold only.** The mod loads and does nothing.

## Target

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.248 |
| Java | 21 |

1.21.1 is the version large tech mods stayed on, so it is where this mod is useful.

## Design

A block holding one kind of item in numbers a chest has no way to express.

Inside it is two values: which item, and how many. The count is not divided into
slots and is not bounded by a stack.

**The care is all at the edge.** An item stack can carry a count past 64, but most
of the paths that touch one — vanilla's and other mods' alike — round it back down
at the far end. That is where every mod of this kind breaks. So the number is kept
inside, and what the block offers outward is an ordinary window: one stack to take
from, one to put into. A hopper sees something it already understands.

Keeping the contents through a break is possible — 1.21 has a data component for
exactly that — but doing it turns the block into an infinite carrying bag, the way
a shulker box is. Whether it does is a setting, and the default is the answer to
"should this replace a shulker box", not to "is it convenient".

## Build

```
run.bat                   # compile and launch a dev client - double-clickable
gradlew build             # produce the jar
gradlew runGameTestServer # run every game test, headless, then exit
gradlew runData           # regenerate models, recipes and language
```

`JAVA_HOME` must point at a JDK 21, or `java` must be on `PATH`.

## Roadmap

- [x] **0** — scaffold; the mod loads
- [ ] **1** — the feature above, in a form that can be watched
- [ ] **2** — checked by game tests rather than by eye

## Related

One of a set of small, independent mods, each doing one thing and depending on
none of the others: [Fodina](https://github.com/Capsicum0907/Fodina),
[Trivium](https://github.com/Capsicum0907/Trivium),
[Magnes](https://github.com/Capsicum0907/Magnes),
[Cella](https://github.com/Capsicum0907/Cella),
[Acervus](https://github.com/Capsicum0907/Acervus),
[Fornax](https://github.com/Capsicum0907/Fornax),
[Accumulator](https://github.com/Capsicum0907/Accumulator).

## License

Not decided yet. Until it is, the metadata says All Rights Reserved.
