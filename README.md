# Acervus

One kind of item, in numbers a chest cannot hold.

*Acervus* is Latin for a heap.

> **Status: stage 5.** Four heaps — Item, Fluid, Energy and Gas — with screens for
> each in the world and in the hand, and a **Horreum** to keep nine of them in.
> 65 game tests pass headlessly, with and without Mekanism installed.

The item one is **Item Heap**, `acervus:item_heap`. It was `acervus:heap` while it
was the only one; being written first is not a reason to be the one without a
surname. **Anything placed under the old name is gone** — there is no data fixer,
and there will not be one before the first release.

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

**The window is one slot that tells the truth.** It reports the whole count, and
taking from it takes as much as was asked for. Both halves matter, and one of them
is a deliberate departure from what the `IItemHandler` javadoc says:

- `getStackInSlot` — "the result's stack size **may** be greater than the itemstack's
  max size." Kept. Rounding it down to a stack is not caution, it is a lie: it is why
  an external storage reading an InfChest reports sixty-four of something there are a
  hundred thousand of.
- `extractItem` — the result "must be less than or equal to `amount` **and**
  `getMaxStackSize()`." **Not kept.** Only `amount` bounds what comes out.

Breaking the second one is not an oversight, it is the convention. InfChest's own
handler is `totalCount().min(amount)` with no stack clamp, and that is exactly why a
pipe with an unlimited upgrade can empty one; keeping the clause instead caps a heap
at one stack per call, which against a pipe that asks once a tick is one stack a
tick. There is no honest way to buy that back — dividing the contents across more
slots does it, but it makes a heap present itself as a chest, and nothing else in
this corner of the ecosystem works that way.

**None of that is enforced by anything.** `IItemHandler` is an interface with
javadoc, and no code checks the returned size; `ItemStack` counts in a plain int, so
two billion in one stack is simply legal. What is real is narrower and worth knowing
exactly:

| | |
|---|---|
| enforced | `ItemStack.count` is an int — 2,147,483,647 and no further |
| enforced | vanilla container slots clamp to `getMaxStackSize()` **when storing**, so an oversized stack put into an ordinary chest loses the excess |
| not enforced | everything in the javadoc above |

So the guardrail is absent rather than present, and the guarantee has to come from
discipline instead. Which is why the next section is the one that matters.

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

Every side is also handed **the same handler object**. Two handlers for one heap
would be two places a stale answer could live.

The last of those is the one [InfChest shipped](https://github.com/Kotori316/InfChest/commit/52aec050)
and fixed in 21.8.1, and it is the reason `spillsNothingWhenBroken` destroys a real
block in a real world and counts what lands on the floor. There is no concurrency to
guard against — block entities are all touched on the server thread — so what looks
like a race is really one of the rows above.

## Using one

**Right-click it.** The screen is laid out like the other three heaps': what it
holds, a bar, the count against the capacity, and a slot on each side.

| | |
|---|---|
| click **In** holding something | put that in |
| right-click **In** holding something | put one in |
| click **Out** | take a stack |
| right-click **Out** | take half a stack |
| shift-click **Out** | move out as much as will fit in the inventory |
| shift-click something of yours | put all of it in |

None of that is written by this mod. It is the game's own handling of a slot, and
the reason the heap is presented as slots: **it is an item, so it should be handled
the way items are handled.** An earlier version had buttons instead — take a stack,
take one — and buttons are a new thing to learn for something the player could
already do.

Out in the world, without opening anything:

- **Right-click holding what it already holds** — put that stack in.
- **Sneak + right-click holding what it already holds** — put in every one of them
  you are carrying.

Holding anything else, or nothing, opens the screen. The question is whether the
heap *already holds* this, not whether it *would take* it: an empty heap would take
anything, and a player who right-clicked to look inside would have committed the
block to whatever was in their hand. Deciding what an empty heap is for belongs in
the screen, or to a pipe — a pipe deciding is the point of a pipe.

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

### Fluid, Energy and Gas heaps

These three have the item heap's screen: what the heap holds, a bar, and the amount
against the capacity (`12.3K B / 1G B`). Their two slots take a container — a
bucket, a tank, a battery.

| slot | what it does |
|---|---|
| **In** (left) | empties the container into the heap |
| **Out** (right) | fills the container from the heap |

The slot decides the direction, not how full the container is, so a half-full tank
can go either way. A container that cannot move any more stays in its slot until it
is taken out. Shift-clicking a container from the inventory puts an empty one in
**Out** and any other in **In**.

**Click the bar** to change what its length means. Pointing at it says which one is
shown. The choice is kept per player, in the client config (`screen.barScale`).

| scale | the bar is full at |
|---|---|
| **Linear** (default) | the capacity — `600G / 1T` is six tenths |
| **Logarithmic** | the capacity, with one tick per power of ten — `1K / 1T` is a quarter |
| **Within the decade** | the next power of ten — `55K` is half of the way from `10K` to `100K` |

Within the decade, pointing at the bar names the decade as powers of ten: `10⁴ - 10⁵`.

In the world, a fluid or gas heap's glass is filled with what it holds. An energy
heap is a closed casing that faces whoever placed it, with twelve lamps on its front:
one more lights for every twelfth of the powers of ten up to the capacity — with the
default of a trillion, one for every tenfold. Every heap writes its amount on each
open side, flat like a sign; the energy heap only on its front.

### Carrying one

A heap keeps its contents when it is broken, so a heap in an inventory is a full
heap that nothing was reading. Now something does: **a carried heap collects.**

- **Walk over what it already holds** and it goes into the heap instead of into a
  slot, sound and animation and all — before the inventory ever sees it, so a full
  inventory is no obstacle.
- **Anything that arrives in a slot another way** — `/give`, a crafting result, a
  shift-click out of a chest — is swept up a moment later. What a heap holds stops
  taking up slots at all.
- **Sneak and right-click the air** to open the one in your hand: the block's own
  screen, same slot and same numbers.

All four heaps open that way, not only the item one. A held fluid, energy or gas
heap reads exactly as the block does, and **In** still takes a container — a bucket,
a battery, a tank empties into it. **Out** takes nothing while the heap is held.

**What is in your hand is left alone**, and that is the escape hatch: it is the one
place to keep something a heap would otherwise claim. Worn armour is left alone for
the same reason, and nothing is swept while a container is open — you are moving
things about on purpose then, and one of them may be a heap.

**It only takes.** Nothing comes back out of a heap until it is placed again — the
screen over a held one says *Deposit only* and means it — and that asymmetry is the
whole design rather than an unfinished half of it:

- Taking is what makes it worth a slot. A stone heap in the hotbar turns a mining
  trip into one slot that never fills, which is the job this mod exists to do.
- Giving would end the game. Two billion of anything reachable from a pocket, with
  no block to place and nothing to stand next to, makes every other kind of storage
  — and most of the reason to carry anything — pointless. Putting the block down is
  the price of drawing from it, and it is a small, deliberate act in a place.

The rules, and why each one is there:

| | |
|---|---|
| only a heap that **already holds** that item takes it | an empty one in a bag would commit itself to whatever you stepped on first |
| only a heap **on its own** in a slot takes anything | several heaps in one slot share one set of components, so filling "the" heap would fill all of them |
| a stack is **shared** between heaps, not offered to each | ten items must stay ten however many heaps are asked |
| items just thrown, or held for someone else, are **left alone** | the same two conditions vanilla checks, checked one step earlier |
| there is **no handler on the item** | so no pipe, backpack or other mod can find a way to drain one from a slot either |

The one exception is the screen: **an empty held heap can be committed there on
purpose.** Opening it and putting something in is a decision; walking over a flower
is not, which is why only the deliberate path may pick what an empty heap is for.

Off with `absorbsWhenCarried = false` under `[item]`.

The screen is the block's, not a second one. `Pile` is what a heap of items is —
the arithmetic, with no opinion about where it is kept — and `HeapBlockEntity` and
`CarriedHeap` are the two places it can be kept. The slot, the menu and the screen
were written against the block; they are written against `Pile` now, so the same
three work over either and `gives()` is the single method that separates them.

## The Horreum

A rack that holds nine heaps and gives one place to reach all of them. The idea is
Industrial Foregoing's [Black Hole Controller](https://ftb.fandom.com/wiki/Black_Hole_Controller):
not a thing that scans for storage nearby, but a box you put the storage *into*.

It fits here almost for free, because **a heap already carries its contents as an
item**. So a rack stores nothing of its own — it is nine slots holding heap items,
and every window it offers is a view over what is in them. Nothing is copied in,
nothing has to be kept in step, and pulling a heap out takes its contents with it
because they were never anywhere else.

A rack from when it held twelve keeps the heaps past the ninth waiting inside it.
Each one moves into the next slot that is emptied, and they travel with the rack
when it is broken.

**Mixed on purpose.** One rack takes item, fluid, energy and gas heaps side by side
and offers the matching window for each: an item handler, a fluid handler, an energy
storage and — where Mekanism is installed — a chemical handler, all on the same
block. Sorting them into four racks would be four blocks and four sets of pipes to
say one thing.

| | |
|---|---|
| items | **one window slot per rack slot**, always nine, so the indices a pipe remembers do not move when a heap is taken out |
| fluids, gases | **one tank per heap actually in it** — `fill` and `drain` take no index, so nothing holds one between ticks |
| filling a fluid or gas | goes to a heap that **already holds it** before it commits an empty one |
| energy | **one pool** — `IEnergyStorage` has no index at all, because energy has no kinds |
| breaking one | carries its heaps, for the same reason a heap carries its contents |

Items keep fixed slots and fluids do not, and the interfaces are what settle it.
`IItemHandler` takes a slot number *when it inserts and extracts*, so a pipe can pick a
slot in one tick and act on it in the next — the numbering has to hold still.
`IFluidHandler` takes none: `fill` and `drain` are given a resource and an amount, and
the index only ever reads a tank within the tick it was asked for. Twelve fixed tanks
was the cautious answer, and it drew eleven bars saying Empty forever in anything that
lists a block's tanks.

**Sneak and right-click the air** to open one you are carrying, the same as a heap.
The heaps inside can be moved about; what is inside *them* stays out of reach, because
nothing carried offers a window onto its contents — taking a heap out of a bag is
moving an item, not drawing from a store.

**The heaps inside give**, unlike the one in your pocket. That was never about the
item: the price of drawing from a heap is putting a block down, and a rack is that
block. It is one field, set by whichever factory found the heap.

### Contents in the tooltip

Every heap draws what it holds: the item itself, or a fluid's or a chemical's own
sprite, beside its name and its amount. A rack draws one such row per heap, so nine
heaps can be read without opening anything.

It is a picture rather than a list of lines because a rack has nine of them, and
nine lines of prose is a tooltip nobody reads past. Energy has no item and no sprite
of its own, so it gets a bolt drawn for it — a row with a hole where every other row
has a picture reads as broken rather than as empty.

**How a rack's heaps are written down is known in one place**, `HorreumBlockEntity`,
and that was bought. Four places spelled the shape out for themselves and the carried
rack spelled it differently — it wrote the item list as the whole component instead of
nesting it under `Heaps`. One write and the other three found nothing, so taking a
heap out of a rack in your bag appeared to destroy the other eleven. They were still
there; nothing could read them. It is the same mistake the count made when it was
`Count` on one heap and `Amount` on three: a shape known in more than one place is a
shape that will disagree with itself.

The reading happens on the client, in `HeapContentsTooltip`, and not where the tooltip
is asked for. `Item#getTooltipImage` is handed a stack and nothing else, and reading a
heap needs the registries — which a client has and an item does not. So the component
carries the stack, and only the stack.

## Optional mods

Acervus needs **nothing but NeoForge**. Mekanism and Refined Storage are things it
notices, not things it requires — and both are checked by running the tests with the
mods folder emptied, not by reading the code and hoping.

**Refined Storage** gets one thing: an external storage provider that reads a heap in
longs. RS counts in longs everywhere — `ResourceAmount` carries one, `insert` and
`extract` take one — and a heap holds a long, so the two agree perfectly. Until now
they had to speak through `IItemHandler`, whose `ItemStack` counts in an int, and a
network looking at a heap of five billion was told 2,147,483,647 and believed it.
Nothing about RS was the limit; the adapter between them was. Point an External
Storage at a heap or at a Horreum and the network is told the real total.

RS publishes no API artifact — not to Central, not to ModMaven — so the dependency is
its own jar off Modrinth's maven, pinned to a version. The factory always returns a
provider, never null, because RS collects them with
`.map(factory -> factory.create(…)).toList()` and does not filter what comes back; one
that answers "nothing here" for somebody else's block is the shape the interface
actually asks for.

That is easy to say and was not true until it was tested. `GasHeap.present()` read
as exactly the right guard and could not work: calling a static method initialises
the class it is on, and `GasHeap` holds a `BlockCapability<IChemicalHandler, …>` in a
static field — so asking "is Mekanism here?" loaded a Mekanism class to find out, and
without Mekanism the mod died during construction. **The guard cannot live behind the
door it is guarding.** It lives in `Mods` now, which names nothing belonging to
anybody else.

The second one was quieter. The `mineable/pickaxe` tag listed the gas heap as a
required entry; a required entry naming a block that does not exist does not go
missing on its own — the whole tag file is refused, and all four blocks fall out of
it together. Without Mekanism a pickaxe was no quicker at a heap than a fist. The
entry is optional now, and written unconditionally, which also ends the trap that the
generated files depended on what happened to be sitting in `run/mods`.

Both were found by taking the mods out of the folder and running the tests, which is
worth doing again whenever a new one is leaned on.

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
- [x] **4** — a carried heap collects: what is walked over, what arrives in a slot,
  and a screen of its own on sneak + right-click. It only ever takes
- [ ] **5** — open questions below

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
[Caldarium](https://github.com/Capsicum0907/Caldarium).

## License

Not decided yet. Until it is, the metadata says All Rights Reserved.
