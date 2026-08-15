# Acervus

One kind of item, in numbers a chest cannot hold.

*Acervus* is Latin for a heap.

> **Status: stage 2.** The heap works and six game tests pass headlessly.
> The drawing has not been watched in a running client.

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
inside, and what the block offers outward is an ordinary window: one slot, holding
one stack at most, that gives back what it is asked for a stack at a time. A hopper
sees something it already understands, and so does every pipe from every mod,
because they all ask the same question — the item handler capability — and none of
them ask anything else.

The same window faces every side. A heap has no front, and sides that only take or
only give would be a second thing to explain for no gain.

**What is inside rides on the dropped block.** Not a setting, and not a choice: at
capacity the contents are two billion items, and spilling those on the floor is
thirty-one million entities. There is no version of that which ends well. So a heap
carries its own contents the way a shulker box does, and is a thing you move rather
than a thing you empty first. The lever for keeping that from becoming a pocket
warehouse is capacity, which *is* a setting — not the drop behaviour.

Because of that, the heap as an item says what is in it on its tooltip. Without
that line a heap holding two billion diamonds looks exactly like an empty one, and
the item would simply be lying.

**Contents are drawn on the block, and there is no screen.** What is stored and how
many are drawn facing the camera rather than fixed to a face: four faces would mean
drawing everything four times, and a chosen front would mean the answer depends on
which way it was placed. Turning the drawing toward whoever is looking costs one
rotation and is right from everywhere.

**An emptied heap forgets what it held.** One that remembered would refuse the next
thing put into it with nothing on the block to say why.

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
- [x] **1** — the heap: storage, the item-handler window, right-click in and out,
  contents drawn on the block, a recipe
- [x] **2** — six game tests, including the two that matter: that the count never
  leaves through the item handler, and that breaking one carries the contents rather
  than spilling them. Left open: watching the drawing in a client
- [ ] **3** — open questions below

## Open questions

- **The recipe.** Currently four glass, four iron and a chest — a chest you can see
  into, held together with iron. Deliberately not gated behind anything rare, on the
  grounds that how much it holds is a setting, so the recipe decides *when* it
  becomes available rather than how strong it is. If it should feel like a
  commitment, the centre is the place to raise: a diamond, or a shulker box.
- Whether one heap should be upgradeable in capacity rather than every heap holding
  the configured maximum.
- Whether a heap should be able to be locked to a kind while empty, so an automated
  line cannot fill it with the wrong thing first.

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
