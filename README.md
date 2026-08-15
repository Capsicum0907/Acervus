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
inside, and what leaves is always an ordinary stack.

Because of that, the count is a **long**. Nothing outside ever holds it, so nothing
outside has to be able to; the only care needed is at the three places the game asks
in ints, where the answer is clamped on the way out. Two billion is not far away
once a factory is running.

**The window has two slots, and they are not symmetric.** One is what a heap gives
out and the other is what it takes in, because the two questions have different
honest answers:

- *Taking out* must never show a count past a stack, for the reason above.
- *Putting in* must show the real room left. A great many pipes work out how much
  fits as `limit − count` rather than by asking, and a single slot that answered
  "sixty-four, and sixty-four are already there" would look full to them while
  holding a thousand. The room is reported on a slot that is always empty, so a
  large number is never attached to an item stack.

Both face every side, and every side is handed **the same handler object**. Two
handlers for one heap would be two places a stale answer could live.

**This block is deliberately not a `Container`.** A hopper prefers the container
path over the item handler when a block offers both, and a container that tries to
describe billions in slots of sixty-four is exactly the shape that goes wrong.
[InfChest](https://github.com/Kotori316/InfChest) had to ship a mixin to force
hoppers off that path; not being a container at all is the same fix, made earlier.

**A pickaxe is the quick way to break one, not a condition for it.** A block of
metal and glass invites `requiresCorrectToolForDrops`, and this one had it — with
the block in no mining tag, which meant no tool was ever correct and a heap dropped
nothing at all. Even done properly it is the wrong trade: the contents are not
replaceable, and forgetting a pickaxe is not a reason to destroy two billion items.
So the block is in `mineable/pickaxe` for speed and requires nothing for drops.

**What is inside rides on the dropped block.** Not a setting, and not a choice: at
capacity the contents are two billion items, and spilling those on the floor is
thirty-one million entities. There is no version of that which ends well. So a heap
carries its own contents the way a shulker box does, and is a thing you move rather
than a thing you empty first. The lever for keeping that from becoming a pocket
warehouse is capacity, which *is* a setting — not the drop behaviour.

Because of that, the heap as an item says what is in it on its tooltip. Without
that line a heap holding two billion diamonds looks exactly like an empty one, and
the item would simply be lying.

**Contents are drawn on the block.** What is stored and how many are drawn facing
the camera rather than fixed to a face: four faces would mean drawing everything
four times, and a chosen front would mean the answer depends on which way it was
placed. Turning the drawing toward whoever is looking costs one rotation and is
right from everywhere.

**Nothing is created and nothing is lost, however many sides are working at once.**
Storage blocks of this kind duplicate for a small number of reasons, and each one is
answered here and then pinned by a test rather than argued:

| how it happens | what stops it |
|---|---|
| a simulated answer that differs from the real one | both are worked out from the same state, at the moment they are asked |
| an insert that quietly shrinks the stack it was handed, so the caller keeps it *and* the heap gains it | the offered stack is never touched; only the remainder is returned |
| per-side handlers that each remember their own version | there is one handler, and it remembers nothing |
| a break that spills the contents while the dropped block also carries them | the block is not a container, so there is nothing to spill |

The last of those is the one [InfChest shipped](https://github.com/Kotori316/InfChest/commit/52aec050)
and fixed in 21.8.1, and it is the reason `spillsNothingWhenBroken` destroys a real
block in a real world and counts what lands on the floor. There is no concurrency to
guard against — block entities are all touched on the server thread — so what looks
like a race is really one of the rows above.

## Using one

**Right-click it.** The screen has a slot, and the slot behaves like a slot:

| | |
|---|---|
| click it | take a stack |
| right-click it | take half a stack |
| click it holding something | put that in |
| shift-click it | move out as much as will fit in the inventory |
| shift-click something of yours | put all of it in |

None of that is written by this mod. It is the game's own handling of a slot, and
the reason the heap is presented as one: **it is an item, so it should be handled
the way items are handled.** An earlier version had buttons instead — take a stack,
take one — and buttons are a new thing to learn for something the player could
already do.

Out in the world, without opening anything:

- **Right-click holding something** — put that stack in.
- **Sneak + right-click holding something** — put in every one of them you are
  carrying.

"A stack" always means whatever a stack of that item is, never sixty-four. Mods that
change stack sizes are common enough that a literal would be wrong under them.

**Numbers are written two ways.** A block seen across a room, and the screen at a
glance, show a size: at most three digits, at most one decimal, and a unit — `100M`,
`2.1G`, `999.9T`. Anything shortened can be asked about by pointing at it — the
count, the room left, the slot — and answers with the number itself,
`2,000,000,000`. A number that has been abbreviated should always be able to say
what it stands for, or the abbreviation is a loss rather than a summary.

The units are SI — K, M, G, T, P, E — which covers the whole range of a long in six
single letters; `Long.MAX_VALUE` is about `9.2E`. The English short scale reads more
naturally at the low end but runs out at T and continues into spellings nobody
knows. These are also what AE2 puts on stored item counts, so anyone who has used
one has already learnt them.

Three digits is also what makes grouping unnecessary in the short form: there is
never a fourth digit to separate. Both forms live in one place, `Counts`, because a
block and its screen disagreeing about the same contents is the kind of thing nobody
notices and everybody distrusts.

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
- [x] **2** — ten game tests, including the ones that matter: that the count never
  leaves through the item handler, that a heap past a stack still looks open to
  pipes, that nothing is created under interleaved access from all six sides, and
  that breaking one leaves a single item on the floor
- [x] **3** — a screen, because a block whose only controls are undocumented
  gestures is a block nobody can use. Built on a real slot rather than buttons, so
  there is nothing new to learn
- [ ] **4** — open questions below

## Open questions

- **The recipe.** Currently four glass, four iron and a chest — a chest you can see
  into, held together with iron. Deliberately not gated behind anything rare, on the
  grounds that how much it holds is a setting, so the recipe decides *when* it
  becomes available rather than how strong it is. If it should feel like a
  commitment, the centre is the place to raise: a diamond, or a shulker box.
- **Whether left-click on the block should take things out**, the way Storage
  Drawers does. It reads well, but left-click is also how a block is broken, and a
  heap that swallows the break is a heap that cannot be picked up. Resolving it means
  giving breaking another gesture — sneak + left-click — and that is a real cost to
  weigh against a screen that already does the job.
- Whether the world-side count should abbreviate at a lower threshold than the
  screen's. They share one rule at the moment, which is simple but means the block
  is as terse up close as it is from across a room.
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
