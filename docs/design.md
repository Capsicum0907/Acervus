# Acervus design notes

[日本語](design.ja.md)

Why the code is the way it is, file by file. These notes used to be comments in the source;
the source no longer carries any.

## `Acervus.java`

**`public class Acervus`**

Entry point. `MODID` must match `mod_id` in gradle.properties,
which is what the generated neoforge.mods.toml is filled from.

**inside, at `if (Mods.mekanism())`**

Loading the class is what registers it, so the check has to come first.

**inside, at `NeoForge.EVENT_BUS.addListener(Carried::onPickup)`**

The game bus, not the mod bus: this one happens while playing rather than while loading. A carried heap has no block entity to tick, so the only moment it can act is the moment something is walked over.

**`private static void registerCapabilities(RegisterCapabilitiesEvent event)`**

The one thing that makes hoppers, droppers and every mod's pipes work: the item
handler capability. They all ask a block for this and none of them ask for
anything else, so getting it right once is the whole of the integration.

Registered without regard to side — a heap offers the same window in every
direction — and handing back the block's one handler rather than a fresh one
per ask, so six sides cannot become six opinions about the same contents.

**inside, at `event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, AcervusRegistry.HORREUM_ENTITY.get(),`**

The rack offers all of them at once: what a pipe finds depends on what is in its slots, not on which block it is talking to.

**`public static class Client`**

Drawing is a client concern, and this is the only place that knows it exists.

## `AcervusConfig.java`

**setting `item.capacity`**

Items in one heap. The default is two billion; the ceiling is what a long counts.

**setting `item.absorbsWhenCarried`**

Whether a heap being carried takes up items of the kind it already holds. It only takes. Nothing comes back out until the heap is placed again.

**setting `fluid.capacity`**

Millibuckets in one heap. The default is a billion buckets.

**setting `energy.capacity`**

Forge Energy in one heap. The default is a trillion.

**setting `energy.pushes`**

Whether a heap offers energy to the blocks touching it. On, because energy is pushed rather than fetched. Items and fluids are the other way round, which is why only this one has the setting.

**setting `energy.pushRate`**

How much it offers each neighbour per tick. Capped at about two billion regardless: that is what one call can carry.

**setting `chemical.capacity`**

Mekanism chemicals in one heap. Only used when Mekanism is installed. The one resource with no ceiling at the edge: Mekanism counts in longs too.

**setting `display.showsContents`**

How a heap looks. Nothing here changes what it does.

**`public final class AcervusConfig`**

Every tunable value lives here. Nothing else in the mod may hold a literal.

SERVER, not COMMON: what a heap holds is world state. A client gets the host's
values.

Grouped by resource, one section each, and the comments are kept to a line or
two. A config file is read while looking for one setting, not read through — the
reasoning behind these numbers belongs in the README, and putting it here instead
turned the file into a wall nobody could find anything in.

**`private static ModConfigSpec.Builder pop()`**

Closes the section just written, so each block above reads as one section.

## `AcervusRegistry.java`

**`public final class AcervusRegistry`**

Registration. One block, its item, and its block entity.

**`public static Item.Properties carriesItsOwnContents()`**

What every item in this mod is made with.

Deliberately plain: a heap stacks to sixty four *while it is empty*, and
the rule that takes that away the moment it holds something is in
`ContentsBlockItem`, where it can be asked of the item rather than fixed
here for all of them at once.

The method stays even though it adds nothing, because every item here has to
go through `ContentsBlockItem` and this is the one line that says so.

**`public static final DeferredBlock<HeapBlock> HEAP`**

`noOcclusion` because the contents are drawn inside it, and a block that
declares itself solid has its neighbours' faces culled against it — including
the ones this block wants to be seen through.

**No `requiresCorrectToolForDrops`.** It is the natural thing to
write for a block of metal and glass, and it is wrong here: a heap that fails
to drop is a heap whose entire contents are gone, and no amount of "you should
have brought a pickaxe" makes losing two billion items a reasonable outcome. A
pickaxe is still the right tool — the block is in `mineable/pickaxe`, so
it is what breaks one quickly — but being without one costs time, not the
contents.

**`public static final DeferredBlock<FluidHeapBlock> FLUID_HEAP`**

The same block, for a resource that is measured rather than counted.

**`public static final DeferredBlock<EnergyHeapBlock> ENERGY_HEAP`**

The same block again, for the resource that has no identity at all.

**`public static final DeferredBlock<HorreumBlock> HORREUM`**

The rack: a block that holds heaps and offers one place to reach all of them.

Solid, unlike the heaps — there is nothing to see through, because what it
holds is heaps rather than contents, and each of those says what is in it on its
own tooltip. No `requiresCorrectToolForDrops`, for the reason every block
in this mod goes without it: failing to drop would take everything inside with it.

**`public static final DeferredHolder<MenuType<?>, MenuType<HorreumMenu>> CARRIED_HORREUM_MENU`**

The same rack in a hand; see `CARRIED_HEAP_MENU`.

**`public static final DeferredHolder<MenuType<?>, MenuType<HeapMenu>> CARRIED_HEAP_MENU`**

The same menu and the same screen, over the heap in a hand rather than the one in
the world. Two types because the two carry different things across the wire — a
position, or which hand — and one type cannot read both.

**`public static final DeferredHolder<MenuType<?>, MenuType<ReadoutMenu>> READOUT_MENU`**

One menu for the three heaps whose contents are not items.

**`public static final DeferredHolder<MenuType<?>, MenuType<ReadoutMenu>> CARRIED_READOUT_MENU`**

The same readout over the one in a hand; see `CARRIED_HEAP_MENU`.

**beside `@SuppressWarnings("DataFlowIssue")`**

the vanilla builder wants a data fixer type it never uses

## `AcervusTests.java`

**`public final class AcervusTests`**

What a heap holds, and — the part that actually breaks in blocks like this — what
it lets out through the item handler that hoppers and pipes talk to.

Run with `gradlew runGameTestServer`.

**`private static final BlockPos OTHER`**

A second heap, to pipe the first one into.

**`private static final int MANY`**

Far past a stack, and past a shulker box, so nothing accidental can pass.

**`private static final int HOTBAR_FIRST`**

The first hotbar slot in a heap menu: the heap's own slot, then three rows.

**`public static void tellsTheTruthAndGivesWhatIsAsked(GameTestHelper helper)`**

A heap says how much it really holds, and gives out as much as it is asked for.

Both halves are what makes the ecosystem work, and one of them is a
deliberate departure from the `IItemHandler` javadoc. Reporting the true
count is explicitly allowed and is the difference between an external storage
reading a hundred thousand and reading sixty-four. Handing out more than a
stack is *not* allowed by the written contract and is what every mod
that moves large amounts relies on anyway — InfChest's own handler is
`totalCount().min(amount)` with no stack clamp, which is why a pipe with
an unlimited upgrade can empty one.

**`public static void givesNoMoreThanWasAskedFor(GameTestHelper helper)`**

Asking for a stack still gets exactly a stack: the amount is what bounds it.

**`public static void fillsAndDrainsThroughTheHandler(GameTestHelper helper)`**

The path a hopper and every mod's pipes actually take.

**`public static void staysOpenToPipesWhenPastAStack(GameTestHelper helper)`**

A great many pipes work out how much fits as `limit - count` instead of
asking. If the only slot showed a full stack, a heap holding a thousand would
look full to them and quietly stop accepting.

**`public static void createsNothingUnderInterleavedAccess(GameTestHelper helper)`**

Nothing is created and nothing is lost, however many sides are working at once.

Storage blocks of this kind duplicate for three reasons, and this pins all
three: a simulated answer that differs from the real one, an insert that
quietly modifies the stack it was handed — so the caller keeps it *and* the
heap gains it — and per-side handlers that each remember their own version of
the contents. Every side is asked for its handler separately here, and the
total is checked against what went in.

**`public static void emptyForgetsItsKind(GameTestHelper helper)`**

An emptied heap forgets what it held. One that remembered would refuse the next
thing put into it, with nothing on the block to say why.

**`public static void carriesItsContentsWhenBroken(GameTestHelper helper)`**

Breaking a heap must not spill what is inside: at capacity that would be tens of
millions of item entities. What it holds rides on the dropped block instead.

**`public static void tellsTheClientItIsEmpty(GameTestHelper helper)`**

An emptied heap has to have something to say. An update packet carrying an empty
tag is thrown away before the block entity sees it, so a heap that wrote nothing
when empty would keep being drawn holding what it no longer holds — which is
exactly what it did until this was pinned.

**`public static void spillsNothingWhenBroken(GameTestHelper helper)`**

Breaking a heap must leave one thing on the floor, not two answers to the same
question.

This is the duplication bug InfChest shipped and had to fix in 21.8.1: its
block entity's superclass dropped the inventory when the block was removed,
while the dropped chest was also carrying everything, so breaking one gave both
copies. The heap avoids it by not being a container in the first place — but
"avoids it by construction" is exactly the kind of claim that stops being true
quietly, so it is pinned here rather than argued.

**`public static void neverVoidsItselfForWantOfAPickaxe(GameTestHelper helper)`**

Breaking a heap with the wrong thing must cost time, never the contents.

A block of metal and glass invites `requiresCorrectToolForDrops`, and
this one had it. With the block in no mining tag that meant no tool was ever
correct, so a heap dropped nothing at all — two billion items gone to a
mis-aimed swing. Even done properly it is the wrong trade here: the contents
are not replaceable and a forgotten pickaxe is not a reason to destroy them.

**`public static void writesNumbersShortAndLong(GameTestHelper helper)`**

Three digits, one decimal, one unit — and the awkward case in the middle.

Rounding is where a rule like this goes wrong: 999,999,999,999 is not far
enough to be a trillion, but rounded to one decimal it reads as 1000.0 of the
unit below, which is four digits and the wrong unit. It has to grow into the
next one instead.

**`public static void anEmptyHeapWaitsForAPipeNotAGlance(GameTestHelper helper)`**

An empty heap is not committed by being right-clicked.

A pipe deciding what an empty heap is for is the point of a pipe. A player
who right-clicked to look inside, and found the block silently committed to
whatever was in their hand, has been caught by the same rule rather than served
by it — so the two ask different questions.

**`public static void handsOutNothingItStillOwns(GameTestHelper helper)`**

Nothing handed out is the heap's own copy of anything.

This is the shape that duplicates by the billion rather than by the stack.
If a returned stack points at internal state, then every `grow` a pipe
performs on it lands inside the block — and a pipe does that many times a tick,
so the count runs away at a rate no single call could explain. InfChest's
handler ends with `item.setCount(...); return item;` under a comment
saying it is "safe to modify as item is already copied", which is a comment one
writes after finding out.

**`public static void simulatingChangesNothing(GameTestHelper helper)`**

Asking what would happen must not make it happen, at either end.

**`public static void survivesPullingIntoItself(GameTestHelper helper)`**

A pipe that pulls out of a heap and puts straight back into it. The loop that
turns a small mistake into billions a second, run against itself.

**`public static void twoHeapsInALoopConserveEverything(GameTestHelper helper)`**

Two heaps piped into a loop, moved at the largest amount there is.

This is the setup that duplicated an InfChest by the billion: two of them
connected so that everything goes across and straight back, every tick, with an
upgrade that asks for all of it at once. A single call being slightly wrong is
invisible; the same call in a loop at two billion a tick is not.

The grand total is what is checked, because in a loop that is the only thing
that means anything — either heap can legitimately be full or empty at any
point.

**`private static void move(IItemHandler from, IItemHandler into)`**

One pipe's worth of work: take everything offered, put back whatever would not fit.

**`public static void saturatesRatherThanWrapsPastTwoBillion(GameTestHelper helper)`**

Past two billion the window saturates, and saturating is the whole requirement.

An item stack counts in an int, so an item handler cannot say a number larger
than `Integer.MAX_VALUE` however much is really there. That much is the
old API's ceiling and not something a heap can fix — it is why NeoForge's newer
resource handlers count in longs. What a heap must not do is *wrap*:
reporting a negative count would be worse than reporting a smaller one, and it
is the arithmetic that is easy to get wrong when the total is a long and
everything it is compared against is not.

Underneath, the heap is unaffected: the count is right, and draining works
because each extraction can take another two billion.

**`public static void aFluidHeapFillsPastWhatItCanSay(GameTestHelper helper)`**

A fluid heap holds far past what a fluid stack can count, and says so as far as
it can.

Everything at the edge here is an int — the stack, the tank capacity, fill
and drain alike — so unlike the item side there is no allowance to report more.
What matters is that the ceiling bounds what is *said* and not what is
*held*: a call is not a lifetime, and repeating one fills a heap as far
as its capacity goes.

**`public static void anEnergyHeapSaturatesRatherThanWraps(GameTestHelper helper)`**

An energy heap holds far past what `IEnergyStorage` can report, and every
number it reports saturates rather than wrapping.

This is the strictest of the three: an item handler is allowed to report more
than a stack and a fluid handler at least measures the same unit it moves, but
every single number in the energy interface is an int, including the two that
only describe. So a heap holding a trillion reads as two billion of two billion
— full — to anything that only knows how to ask, while still accepting and
giving out correctly.

**`public static void anEnergyHeapSimulatesWithoutMoving(GameTestHelper helper)`**

Simulating must not move anything, at either end.

**`public static void anEnergyHeapOffersItselfToItsNeighbours(GameTestHelper helper)`**

An energy heap gives what it holds to what is next to it, without being asked.

Pushing is the Forge Energy convention and not a preference: a store offers,
a machine waits. Reasoning from "a heap is a container, so let things come and
take" produced a block that filled happily and never gave anything back to
anything — which passed every test about extraction, because every one of them
did the asking itself.

**`public static void aCarriedHeapTakesWhatIsWalkedOver(GameTestHelper helper)`**

A heap in a pocket takes what is walked over, before the inventory sees it.

The check that matters is the last one: the diamonds are in the heap and
*not* in a slot. Absorbing that also left a copy in the inventory would
be a duplication bug that looks like a feature working.

**`public static void aCarriedHeapTakesOnlyItsOwnKind(GameTestHelper helper)`**

Anything else goes past it, to be picked up the ordinary way.

**`public static void anEmptyCarriedHeapCommitsToNothing(GameTestHelper helper)`**

An empty carried heap claims nothing.

The block lets a pipe decide what an empty heap is for, because that is what a
pipe is. Walking over something is not a decision, and a heap that committed
itself to the first flower picked up would be ruined by a step.

**`public static void severalCarriedHeapsShareOneStack(GameTestHelper helper)`**

Two heaps and ten diamonds are still ten diamonds.

Every heap is asked in turn, so the count left has to travel between them.
Handing each the whole stack would have both report success and turn ten into
twenty — the one way this feature could create items out of nothing.

**`public static void aStackOfCarriedHeapsTakesNothing(GameTestHelper helper)`**

Two heaps in one slot are one set of components, so adding to "the" heap would
add to both. Refusing is the same answer a shulker box gives by not stacking.

**`public static void aCarriedHeapWaitsOutThePickupDelay(GameTestHelper helper)`**

An item that was only just thrown is not free to take yet, for a heap either.

**`public static void whatDoesNotFitIsHandedBack(GameTestHelper helper)`**

What the heap could not hold is handed back for the inventory, and the heap
plus the slot come to exactly what was on the ground.

The whole-stack case leaves nothing behind and is the easy one. This is the
path where the item entity survives the event and vanilla carries on with the
remainder, and it is the one place a number could be counted twice.

**`public static void theStatisticNamesWhatWasPickedUp(GameTestHelper helper)`**

The statistic names what was picked up, not air.

An emptied stack answers `Items.AIR`, and absorbing the whole of a drop
empties it — so reading the item after the shrink would have quietly credited
every full pickup to air for as long as a matching heap was carried. Vanilla
takes the item at the top of `playerTouch` for this reason; so does this.

**`public static void aCarriedHeapGivesNothingBack(GameTestHelper helper)`**

The other half of the rule, and the reason the first half is safe: a carried heap
offers no handler, so nothing — no pipe, no backpack mod, no other heap — can
draw two billion items out of an inventory slot.

**`public static void theSweepTakesWhatArrivedInASlot(GameTestHelper helper)`**

What arrived in a slot some other way is swept up too.

Walking over something is only one of the ways items reach a player.
`/give`, a crafting result and a shift-click out of a chest all put things
straight into a slot, and a heap that collected one and not the others would be
a rule with no shape.

**`public static void theSweepLeavesWhatIsInHand(GameTestHelper helper)`**

What is in the hand is left alone.

It is the one place to keep something a heap would otherwise claim, and it
has to exist: without it, carrying a heap of cobblestone would mean never being
able to hold a cobblestone.

**`public static void theScreenOverAHeldHeapOnlyTakes(GameTestHelper helper)`**

The screen over a held heap shows it and takes deposits, and gives nothing back.

Every way out is closed, not just the obvious one: the slot refuses to be
picked up from, refuses to be shift-clicked out of, and hands back nothing when
asked directly. One of those left open would be the whole rule undone.

**`public static void theHeldHeapIsFrozenWhileItsScreenIsOpen(GameTestHelper helper)`**

And the heap itself cannot be moved out from under its own screen.

**`public static void theScreenCommitsAnEmptyHeldHeap(GameTestHelper helper)`**

An empty held heap can be committed on purpose, which walking over things never
does. The screen is where a decision is made; a step is not a decision.

**`public static void whatWasCollectedSurvivesBeingPutDown(GameTestHelper helper)`**

What a carried heap collected is there when it is put back down.

This is the round trip the rest of the carrying tests do not make: they read
the item's own numbers back, which would agree with themselves even if the shape
written were one no block could load. `CustomData#loadInto` is the same
call `BlockItem` makes when the block is placed.

**`public static void aHeldFluidHeapReadsAndTakes(GameTestHelper helper)`**

A fluid heap in a hand reads what it is carrying, and a bucket put in its slot
empties into it.

Only inward, and not only when full: the block asks whether the container is
full because the block can also pour back out and has to be told which way a
half-empty bucket was meant to go. Here there is one direction and no question.

**`public static void aHeldEnergyHeapReadsAndTakes(GameTestHelper helper)`**

An energy heap in a hand does the same with a battery — charged or not.

A dev-environment battery is hard to come by, so this drives the heap's own
side directly and checks the one thing the vessel cannot: that what goes in stays
in and nothing will come back out.

**`public static void aHeldFluidHeapSurvivesBeingPutDown(GameTestHelper helper)`**

What a held heap collected is there when it is put back down — the round trip the
readings alone cannot make, since they would agree with themselves even if the
shape written were one no block could load.

**`public static void aHeldReadoutHandsBackWhatIsInItsSlot(GameTestHelper helper)`**

The screen over a held readout takes a container and hands it back when it closes.

The vessel belongs to the screen rather than to the item — saving it onto the
heap would mean a bucket could be left inside one in a pocket, which is a second
kind of storage nobody asked for. So closing must not swallow it.

**`public static void everyHeldHeapCanBeSaved(GameTestHelper helper)`**

Every heap an item can be written by hand must survive being saved.

This is the one that was missing, and it cost a world. `BLOCK_ENTITY_DATA`
is persisted with `CustomData.CODEC_WITH_ID`, which refuses a tag that does
not name its block entity — and it refuses it inside the player inventory save, so
the failure is a crash rather than a lost item. Nothing caught it earlier because
the network codec does not check, and because every test read the numbers back
through the same code that wrote them: they agreed with each other about a shape
the game would not accept.

So this asks the game, with the call that crashed.

**`private static void saves(ItemStack stack, HolderLookup.Provider registries, String what)`**

The call the server makes on every player, once a minute, forever.

**`public static void whatGivesIsWhereTheHeapIsKept(GameTestHelper helper)`**

The same heap gives or does not give depending on where the item is.

A pocket does not give; a controller does, because a controller is a placed
block and placing one is the price. The reading code is identical either way —
what differs is one field, set where the heap was found — so this checks that the
field is what decides, on all three, rather than the class.

**`public static void anEmptiedStoredHeapForgetsWhatItHeld(GameTestHelper helper)`**

Drained to nothing, a stored heap forgets its kind and loses the component
outright — so it stacks with the other empty ones again, and the next thing put
into it is not refused for a reason nothing on the item explains.

**`public static void aRackOffersEveryKindItHolds(GameTestHelper helper)`**

A rack offers each kind of heap it holds through the window that kind speaks.

Mixed on purpose: one item heap and one fluid heap in the same rack, reached
through the item handler and the fluid handler respectively. The rack itself
stores nothing, so what this really checks is that reading a slot as a heap and
writing back through it survives the round trip.

**inside, at `check(tanks != null && tanks.getTanks() == 1,`**

One tank per fluid heap in the rack, not one per rack slot: the heap put in slot 3 is the rack's only fluid heap, so it is tank 0. Anything that lists a block's tanks would otherwise draw eleven bars that say Empty forever.

**`public static void aRackFillsWhatItAlreadyHoldsFirst(GameTestHelper helper)`**

Filling looks for a heap that already holds that fluid before it commits an empty
one. The other way round, a rack fills up with half-used tanks of the same thing
while a matching heap stands beside them.

**`public static void aRackOfEnergyHeapsIsOnePool(GameTestHelper helper)`**

Energy has no kinds, so a rack of energy heaps is one pool with one total.

**`public static void aRackSimulatesWithoutMoving(GameTestHelper helper)`**

Simulating must move nothing, at any of the three windows.

**`public static void aRackCarriesItsHeapsWhenBroken(GameTestHelper helper)`**

A rack carries its heaps when it is broken, and they must survive the saving.

The rack's own contents go through `ContainerHelper`, which is a
different path from the player inventory that crashed once already. Reading them
back through the code that wrote them is exactly the check that missed it, so
this asks the game to save the item instead.

**`public static void takingOneHeapOutOfACarriedRackKeepsTheRest(GameTestHelper helper)`**

Taking one heap out of a carried rack leaves the other eleven where they were.

It did not. The carried rack wrote its heaps as the whole component instead of
nesting them under `Heaps` the way the block entity does, so one write made
the other three readers - the block, the tooltip text and the tooltip picture -
find nothing at all. The heaps were never destroyed; nothing could read them.

So this writes with one and reads with the others, which is the only shape of
test that would have caught it.

**inside, at `HorreumBlockEntity placed`**

Read back the way a placed rack reads it, which is the reader that stopped finding anything.

**`public static void everyItemHasARecipe(GameTestHelper helper)`**

Every item this mod registers can be crafted.

Written the way round that survives the mod growing: it asks the item registry
what belongs to Acervus rather than naming five things by hand, so an item added
later and forgotten fails here instead of being noticed in a playthrough.

It reads recipes as the game loaded them, not as datagen wrote them, which is
the difference that matters for the gas heap. Its recipe file ships whether
Mekanism does or not; without Mekanism the item does not exist, so both sides of
this comparison lose it together and the test still passes. What it cannot see is
a recipe file that was *refused* - that leaves no trace in the game, only
an error in the log.

**`public static void emptyOnesStack(GameTestHelper helper)`**

Empty ones stack. That is the half of the rule that is a convenience: sixty four
empty heaps are sixty four empty heaps and there is nothing in them to copy.

Asked of the registry rather than of five names, because the way this comes
back is an item added later that did not go through `ContentsBlockItem`.

**`public static void fullOnesDoNot(GameTestHelper helper)`**

One holding anything stacks to one. That is the half of the rule that is a bug
when it is missing: a stack of two is one set of contents with a count of two, so
filling it fills "both" and splitting it copies what was inside.

Checked on a heap and on a rack, which arrive at their contents by different
routes, and read through `ItemStack` rather than through the item so that
what is asked is what the game asks.

**inside, at `CarriedHeap.stored(registries, heap).extract(64, false)`**

And back again: emptied, it is an empty one like any other.

**`public static void aStackOfHeapsHasNoScreen(GameTestHelper helper)`**

A stack of heaps has no screen, because a screen onto a stack is a screen onto
every heap in it.

The other way in — `Carried.absorb` — has refused a stack since it was
written. This is the one that did not, and it is the one a player reaches by
crafting several and sneak-clicking without splitting them first.

**`public static void aStackHoldingSomethingIsNotPlaced(GameTestHelper helper)`**

A stack that is holding something will not be placed, however it came to be.

This state is unreachable in play — a stack cannot be filled and a full one
cannot be stacked — so the test forces it, the way a command or another mod's
inventory code could. Placing is where the duplication would happen and it needs
no screen and no writing at all: the contents ride on the item and are copied onto
every block placed, while the stack only gets shorter.

**`private static long carriedCount(Player player, int slot)`**

What the heap in that inventory slot is holding.

**`private static ItemStack carried(GameTestHelper helper, ItemStack contents)`**

A heap item holding what a real heap would hold, written the way a broken one is.

## `Carried.java`

**`public final class Carried`**

A heap that is being carried collects.

Two ways in, because items reach a player two ways. One is walked over and
intercepted before the inventory ever sees it; the other is already in a slot —
from `/give`, from a crafting result, from a chest — and is swept up
afterwards. Together they mean the same thing to a player: **what a heap holds
does not take up slots any more.**

Nothing comes back out. `CarriedHeap` is where that rule lives and why.

Only a heap that *already holds* something takes anything, the same
distinction `HeapBlockEntity.holds` draws against `accepts`: an empty
heap would otherwise commit itself to whatever was walked over first, which is a
decision made by accident. Committing an empty one is what its screen is for.

**`public static void onPickup(ItemEntityPickupEvent.Pre event)`**

Takes items out of the air and into a heap the player is already carrying,
before the inventory ever sees them.

Everything past the absorbing is what vanilla would have done: the take
animation and its sound, the statistic, and the pickup trigger. They are done
here because `ItemEntity#playerTouch` only does them when
`Inventory#add` accepted something, and the whole point of this is that
the inventory never did.

**inside, at `if (!event.canPickup().isTrue()`**

The same two conditions vanilla checks after this event, checked before it: an item just thrown, or one being held for whoever dropped it, is not free to take yet. TRUE means another listener has already waived both.

**inside, at `Item taken`**

Read before the shrink. An emptied stack answers Items.AIR, so asking it afterwards would quietly award every full pickup to air — which is why ItemEntity#playerTouch takes the item at the top and not where it is used.

**inside, at `entity.discard()`**

Nothing is left for the inventory, and an item entity holding an empty stack would sit there being collided with forever.

**`public static void onTick(PlayerTickEvent.Post event)`**

Everything that reached a slot some other way — `/give`, a crafting
result, a shift-click out of a chest — swept into the heaps carrying it.

Not while a container is open. The player is moving things about on purpose
then, and one of the things they may be moving them out of is a heap; a sweep
running underneath would put it straight back.

**`public static void sweep(Player player)`**

Absorbs what is in the player's own storage slots.

**Not what is in their hand.** That is the one place to keep something a
heap would otherwise claim, and it needs to exist: without it, carrying a heap
of cobblestone would mean never being able to hold a cobblestone. Worn armour is
left alone for the same reason.

**`public static int absorb(Player player, ItemStack incoming)`**

Puts as much of `incoming` as will fit into the heaps this player is
carrying, without taking it out of the stack.

Every slot is searched for a heap, the hands and the armour included — a heap
held in the hand collecting is what anyone would expect of one. Which slots are
looked at as a *source* is a separate question, answered in
`sweep`.

Returns how many were taken, which is none unless some heap already holds them

**`private static int absorb(Player player, ItemStack heap, ItemStack incoming, int wanted)`**

`wanted`: at most this many, so a caller filling several heaps from one stack
              cannot promise the same items twice
Returns how many this heap took

**inside, at `if (!isHeap(heap) || !ContentsBlockItem.alone(heap))`**

One heap, not a stack of them. Several heaps in a slot are one set of components between them, so adding to "the" heap would add to all of them — the same duplication a shulker box avoids by not stacking at all.

**`public static InteractionResultHolder<ItemStack> open(Player player, InteractionHand hand,`**

The gesture that opens a held heap: sneak and right-click the air.

The air, because right-clicking a block is how a heap is placed and that must
keep working; `useOn` runs first and only a miss reaches here. Sneaking,
because a plain right-click with a heap in hand already means something on the
heap in front of you.

All four heaps do this and only the menu differs, so the gesture is written
once. What opens is the block's own screen, with one difference the screen states
outright: nothing comes out.

**One at a time.** A screen onto a stack of empty heaps is a screen onto all
of them — whatever went in would go into every one, and taking them apart
afterwards would copy it. This is the same guard `absorb` makes, in the
other way in.

**`private static boolean carriesAHeap(Player player)`**

One pass before the nested one, so a player carrying no heap costs almost nothing.

## `CarriedHeap.java`

**`public final class CarriedHeap implements Pile`**

The heap that is being carried, read as a `Pile`.

A heap keeps its contents when it is broken, so a heap in an inventory is
already a full heap — the numbers are sitting in `BLOCK_ENTITY_DATA` with
nothing looking at them. This is the looking, in the same terms the block answers
in, so the slot and the screen do not have to know which one they are over.

**It takes and does not give.** `gives()` is false and every path out
is closed behind it. That asymmetry is the design and not an unfinished half of
it: collecting while mining is what makes a heap worth a slot, and drawing two
billion of anything out of a pocket would end every reason to carry anything else.
Taking still means placing the block somewhere, which is a deliberate act in a
place. Everything below `gives()` is written out in full, so the day that
judgement changes it is one method.

The stack is fetched afresh every time rather than held. The item can be moved,
swapped or dropped while a screen is open on it, and holding the object would mean
writing into a stack that is no longer anywhere.

**`private static final String LEGACY_COUNT`**

What the count used to be called here. Read when the current name is absent, so
heaps saved before the four blocks agreed on one word keep their contents.

**`private CustomData read`**

The last parsed sample, and the data it came from, so a render loop parses once.

**`public static CarriedHeap of(HolderLookup.Provider registries, ItemStack stack)`**

A particular stack, which the caller is holding still.

**`public static CarriedHeap of(Player player, ItemStack stack)`**

The same, where a player is the nearest thing that knows the registries.

**`public static CarriedHeap inHand(Player player, InteractionHand hand)`**

Whatever is in that hand at the moment of asking.

**`public static CarriedHeap stored(HolderLookup.Provider registries, ItemStack stack)`**

A heap slotted into a controller, which is a placed block, so it gives.

**`public boolean gives()`**

Whether anything may come out, decided by *where the item is* rather than
by what it is. See `Held.gives()` for the reasoning; it is the same rule and
the same field.

**inside, at `return isEmpty() || ItemStack.isSameItemSameComponents(sample(), stack)`**

The same answer the block gives, deliberately, including about other heaps: one kind of heap behaving differently from the other is a rule nobody can see.

**`public boolean holds(ItemStack stack)`**

Whether this is already what it holds — the question a person is asked.

**inside, at `tag.remove(SAMPLE)`**

Emptied heaps forget their kind, here as on the block: one that remembered would refuse the next thing put in with nothing to say why.

**`public void setChanged()`**

Nothing to do: the item lives in an inventory, which sends its own changes.

**`private void write(ItemStack heap, CompoundTag tag)`**

An emptied heap loses the component altogether, so that it stacks with the other
empty ones again instead of looking different for carrying an empty tag.

**Through `setBlockEntityData`, never `CustomData.of`.** The
component is persisted with `CustomData.CODEC_WITH_ID`, which refuses a tag
with no `id` naming the block entity — and refuses it while the player's
inventory is being saved, which takes the world down. Nothing catches it earlier:
the network codec does not check, so a heap written by hand looks perfectly well
until the first autosave. See `Held.write`.

## `Compression.java`

**`public final class Compression`**

Which items are the same thing in another form, found from the game's own crafting
recipes. Two items are linked only when both directions exist and cost nothing: four or
nine of the smaller make exactly one of the larger, and one of the larger gives back
exactly that many of the smaller. A recipe that only goes one way, such as quartz into
a quartz block, is not a conversion. An item that would link to two different larger or
smaller forms is left unlinked, because there is no way to say which one was meant.

The scan starts from the recipes rather than from the items. Trying every item in a
grid against every recipe grows with the product of the two, which a large modpack
would feel at every start; reading the recipes first keeps only those made of four or
nine of one thing, and only those are tried both ways.

It runs when the server has started and again whenever `/reload` reloads the recipes.


**Counting an item heap: `Amount` and `Unit`**

An item heap counts in the smallest form its kind comes in, and remembers how many of
those its registered form is worth: `Unit`. A heap of gold ingots holds `Amount`
nuggets and a `Unit` of 9, and everything outside the heap — the screen, pipes, the
capacity — is in ingots, `Amount / Unit`. The capacity is in the registered form, so
counting smaller costs it nothing.

The unit is a ratio to the registered form, not the name of the smallest item, so
nothing breaks when the mod that adds the smallest form is removed. When the chain
changes, the count is rebased to the new unit the next time something goes in or
out: a smaller form appearing multiplies it, and losing one divides it, dropping only
the part that was less than one of the new unit. A heap written before `Unit` existed
reads as a unit of 1.
## `ContentsBlockItem.java`

**`public abstract class ContentsBlockItem extends BlockItem`**

Every item in this mod: one that carries, on the item, what its block was holding.

**Why they share a class at all.** Components belong to the `ItemStack`,
not to each item in it, so two of these in one stack are not two containers — they
are one set of contents with a count of two beside it. Filling that stack fills
"both"; splitting it copies what was inside; and *placing* from it copies the
contents onto every block placed, without anything being written at all. That last
one is the reason a rule about opening screens could not have been enough: placement
duplicates in complete silence.

**The rule: it stacks only while it is empty.** Not "it never stacks" — sixty
four empty heaps are sixty four empty heaps, and carrying them is the whole
convenience. The moment one holds something it is alone, and it goes back to
stacking when it is emptied again.

Derived, never stamped. `getMaxStackSize` *asks* whether the item is
carrying anything rather than having an answer written into it by whoever last wrote
the contents. There are four places that write contents — the two screens, the block
being broken, and middle-click — and a fifth would be added one day without the
stamping being noticed. A question cannot be forgotten the way a statement can.

The one question is `BLOCK_ENTITY_DATA`, which every one of these uses and
which is *removed* rather than emptied when the last of the contents goes —
see `Held.write`. So "is it carrying anything" and "does it stack" are the same
question asked twice, which is why they cannot disagree.

**`public static boolean holdsSomething(ItemStack stack)`**

Whether this item has anything in it, asked of the item and nothing else.

**`public static boolean alone(ItemStack stack)`**

Whether this is one of these rather than a pile of them.

Anything that would put contents *into* an item asks this first, because
a stack cannot be filled without filling all of it. `Carried.absorb` and
`Carried.open` are the two ways in.

**`public InteractionResult place(BlockPlaceContext context)`**

A stack of these that is somehow carrying something will not be placed.

Nothing in normal play can reach this: a stack cannot be filled, and a filled
one cannot be stacked. It is here for the case the rule is broken from outside —
a command that names both the contents and a count, another mod's inventory code
merging without asking `getMaxStackSize` — because placing such a stack is
where the duplication would actually happen, one full block at a time.

Refused out loud rather than quietly placed as an empty one. Losing the
contents without saying so would look exactly like the bug this prevents.

## `Counts.java`

**`public final class Counts`**

How a number of items is written down.

Two forms, and which one is used says what the reader is doing. Glancing at a
block across a room, or at a screen while sorting, wants a size — *about two
billion* — and thirteen digits is not a size, it is a wall. Asking for detail
wants the number itself. So the short form is what is drawn, and the exact form
is what appears when somebody asks.

Both live here rather than at the two places that draw them, because a block
and its screen disagreeing about the same contents is the kind of thing nobody
notices and everybody distrusts.

**`private static final long SMALLEST_UNIT`**

Below the smallest unit the number is already three digits, which is the target.

**`private static final long[] UNITS`**

SI, largest first, and the whole range of a long is covered by six single
letters — `Long.MAX_VALUE` is about 9.2E. The English short scale would
read more naturally at the low end, but it runs out at T and continues into
spellings nobody knows; these are also what AE2 puts on stored item counts, so
anyone who has used one has already learnt them.

**`public static String exact(long count)`**

Every digit, grouped. For tooltips and anywhere the reader asked.

**`public static String brief(long count)`**

At most three digits, at most one decimal, and a unit: `100M`,
`2.1G`, `999.9T`.

Three digits is the point of it: a number that fits in a glance. It is also
what makes grouping unnecessary here — there is never a fourth digit to
separate — while `exact` keeps its commas.

**inside, at `if (tenths((double) count / UNITS[unit]) >= 10_000L && unit > 0)`**

Rounding can push 999.97G up to 1000.0G, which is four digits and the wrong unit. When it does, the number has grown into the next one.

**beside `return Long.toString(count);`**

unreachable: anything at or above a unit found one

**`private static final long PER_BUCKET`**

A thousand millibuckets to the bucket, which is the only reason this exists.

**`public static String buckets(long millibuckets)`**

A quantity of fluid, said in buckets.

Millibuckets are what the game's plumbing counts in, and they cost three
digits of every number for nothing: the same int that counts two billion items
counts two million buckets. The unit is not ours to change, but which unit is
*shown* is, and a player counts buckets.

Below a bucket there is nothing to round to, so those are said as they are.

**`public static String exactBuckets(long millibuckets)`**

Every millibucket of it, for when the detail was asked for.

**`private static String mantissa(double value)`**

One decimal, and no decimal at all when it would be a nought.

## `EnergyHeapBlock.java`

**`public class EnergyHeapBlock extends BaseEntityBlock`**

The energy heap.

No interaction of its own. There is no bucket of electricity to right-click it
with — energy arrives and leaves through cables, and a block that also had a
gesture would be inventing one for a thing that already has a way in.

**`public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,`**

Server side only: pushing is a thing the world does, and the client has no say.

**`protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,`**

Empty-handed: open the readout.

**`public List<ItemStack> getDrops(BlockState state, LootParams.Builder params)`**

What is inside rides on the dropped block, for the reasons in `HeapBlock.getDrops`.

## `EnergyHeapBlockEntity.java`

**`public class EnergyHeapBlockEntity extends BlockEntity implements Heaped, HasVessel`**

What an energy heap holds: a number, and nothing else.

The simplest of the three, and the difference is worth naming: energy has no
identity. An item heap and a fluid heap both keep a sample beside the amount,
because "five thousand" means nothing without "of what" — and both therefore have
to answer what happens when something else is offered. Energy is energy, so this
one has no sample, cannot be locked to a kind, and never refuses anything except
for being full.

**`private static final String AMOUNT`**

The same name every heap uses; see `HeapBlockEntity` for why.

**`public EnergyHeapHandler handler()`**

One handler for the whole block, handed to every side.

**`public int receive(int offered, boolean simulate)`**

Returns how much was taken in, which is never more than was offered

**`public int give(int wanted, boolean simulate)`**

Returns how much was handed out, which is never more than was asked for

**`public Component contentName()`**

Energy has no kinds, so there is nothing to name.

**`public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyHeapBlockEntity heap)`**

Offering what it holds to whatever is touching it, once a tick.

An energy heap pushes; the item and fluid heaps do not. That is not an
inconsistency, it is the ecosystem: a hopper comes and takes items, a pump comes
and takes fluid, and a machine that wants power sits there waiting to be given
some. A store that only answered when asked would sit full beside a furnace that
never asked — which is exactly what this one did until it was watched.

The neighbours are looked up through a cache rather than every tick, because
a capability lookup is a map search and this happens twenty times a second per
heap.

**inside, at `heap.tickVessels()`**

The vessel first, and unconditionally: a battery in the slot is a person asking, and turning pushing off is about cables rather than about them.

## `EnergyHeapBlockItem.java`

**`public class EnergyHeapBlockItem extends ContentsBlockItem`**

The energy heap as an item, saying what it is carrying so that it is not lying.

**`public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)`**

Sneak and right-click the air to look inside the one you are holding.

**`public java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> getTooltipImage(`**

The contents as a picture rather than as a line of text; see `HeapContents`.
Offered for anything at all, and the client decides there is nothing to draw when
the heap is empty.

## `EnergyHeapHandler.java`

**`public class EnergyHeapHandler implements IEnergyStorage`**

The window an energy heap shows to cables and machines.

Every number in `IEnergyStorage` is an int, including the two that only
report — so unlike the item side there is no allowance anywhere to say more than
two billion. Both reported numbers therefore saturate, and a heap holding a
trillion reads as full to anything that only knows how to ask.

That is a display problem rather than a storage one: `receiveEnergy` and
`extractEnergy` are bounded per call, and a call is not a lifetime. What it
does cost is the `stored / capacity` bar every energy readout draws, which
will sit at whatever fraction two billion is of two billion. There is no way to
be both honest and accurate here; saying the largest true-ish number is the less
wrong of the two.

Nothing is remembered here, for the same reason as the other two.

## `FluidHeapBlock.java`

**`public class FluidHeapBlock extends BaseEntityBlock`**

The fluid heap.

Filling and emptying is left entirely to `FluidUtil`, which is what the
game's own tanks use: it works out whether the held thing is a full bucket or an
empty one, which way the fluid should go, how much fits, and what to hand back.
Writing that here would be reimplementing bucket logic in order to get it subtly
wrong for somebody's modded container.

**`public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,`**

Server side only.

**`protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,`**

Empty-handed: open the readout.

**inside, at `return held.isEmpty() ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION`**

The client cannot know whether it worked, and guessing wrong leaves a bucket in the hand that the server has already emptied.

**`public List<ItemStack> getDrops(BlockState state, LootParams.Builder params)`**

What is inside rides on the dropped block, for the reasons in `HeapBlock.getDrops`.

## `FluidHeapBlockEntity.java`

**`public class FluidHeapBlockEntity extends BlockEntity implements Heaped, HasVessel`**

What a fluid heap holds: one kind of fluid, and how much of it in millibuckets.

The same shape as `HeapBlockEntity` and for the same reasons — a long
amount that the block keeps, and a sample carrying only the identity of what is
stored. The two are not yet one class on purpose: a third resource is what will
say which parts are really shared and which only look alike, and guessing that
from two would be guessing.

The ceiling at the edge is lower here than it looks. A `FluidStack`
counts in an int and so does every method of `IFluidHandler`, so what a
fluid heap can *say* stops at about two million buckets however much it
holds. As with items, the answer is to saturate rather than to wrap.

**`private FluidStack sample`**

Identity only: the fluid and its components, always with an amount of one.

**`public FluidHeapHandler handler()`**

One handler for the whole block, handed to every side.

**`public FluidStack contents()`**

Everything inside, saturated at what a fluid stack can count.

**`public boolean holds(FluidStack stack)`**

Whether this is already what the heap holds — the question a person is asked.

**`public int insert(FluidStack stack, boolean simulate)`**

Returns how much of the offered fluid was taken, which may be none

**`public FluidStack extract(int wanted, boolean simulate)`**

Returns what was taken out: as much as was asked for, if there is that much

**inside, at `sample`**

Forgotten with the last drop, so the next thing poured in is not refused for a reason nothing on the block explains.

**`public String brief(long value)`**

Buckets, because millibuckets cost three digits of every number for nothing.

**`protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)`**

The amount is written even when it is zero; see `HeapBlockEntity.saveAdditional`.

## `FluidHeapBlockItem.java`

**`public class FluidHeapBlockItem extends ContentsBlockItem`**

The fluid heap as an item, and the reason it needs its own class: a heap carries
its contents when picked up, so one holding two million buckets looks exactly like
an empty one unless it says otherwise.

**`public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)`**

Sneak and right-click the air to look inside the one you are holding.

**`public java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> getTooltipImage(`**

The contents as a picture rather than as a line of text; see `HeapContents`.
Offered for anything at all, and the client decides there is nothing to draw when
the heap is empty.

## `FluidHeapHandler.java`

**`public class FluidHeapHandler implements IFluidHandler`**

The window a fluid heap shows to pipes and tanks.

One tank, telling the truth about what is in it and giving out what it is asked
for. The difference from the item side is only where the ceiling is: an item
handler is documented as being allowed to report more than a stack, while every
number in `IFluidHandler` is an int and there is nothing above it to report.
So both saturate, but this one saturates because it must rather than because it
chose to.

Nothing is remembered here. Every method reads the block entity when it is
called, for the same reason as the item side: a cached answer is a second copy of
the truth, and two of them disagree.

**inside, at `return heap.holds(resource) ? heap.extract(resource.getAmount(), action.simulate())`**

Asked for a particular fluid: give nothing at all if it is not this one, rather than quietly handing over something else of the same size.

## `HasVessel.java`

**`public interface HasVessel`**

A heap with somewhere to put a container.

Separate from `Heaped` so that the menu can find the slot without naming
any of the three block entities — which matters for the gas one, whose class
cannot be mentioned in a game without Mekanism.

## `HeapBlock.java`

**`public class HeapBlock extends BaseEntityBlock`**

The block. Everything it does is one of two things: putting something in, or
taking something out.

There is no screen. What is inside is drawn on the block itself, which is both
the point of it and the reason a screen would have nothing to show.

**`protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,`**

Holding what the heap already holds: put it in. Sneaking puts in every one the
player has. Holding anything else — or nothing — falls through to the screen.

The question is `HeapBlockEntity.holds`, not `accepts`. An empty
heap accepts anything, and a player who right-clicked to look inside would
silently commit it to whatever was in their hand. Deciding what an empty heap
is for is a thing to do deliberately, in the screen or through a pipe.

**`protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,`**

Empty-handed: open the screen.

Taking things out lives there rather than out here. A block whose only way
of being used is a gesture nobody was told about is a block nobody can use —
which is what this one was until the screen existed.

**`private static int insertEveryMatch(Player player, HeapBlockEntity heap)`**

Every matching stack the player is carrying, in one gesture. Emptying an
inventory one stack at a time is the thing a block like this exists to stop.

**`public List<ItemStack> getDrops(BlockState state, LootParams.Builder params)`**

**What is inside rides on the item.** A heap holds more than a chest by
several orders of magnitude, and there is no version of spilling that onto the
floor that ends well: two billion items is thirty-one million entities. So the
contents are written onto the dropped block, the way a shulker box carries its
own, and a heap is a thing you move rather than a thing you empty first.

This replaces the loot table rather than adding to it, so there is one
answer to what a heap drops instead of two that have to agree.

**`public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target,`**

Picking the block with middle-click should give back what is in it, too.

## `HeapBlockEntity.java`

**`public class HeapBlockEntity extends BlockEntity implements Pile`**

What a heap holds: one kind of item, and how many.

The count is a plain int rather than a list of slots. A slot is a place to put
up to a stack, and this block's entire reason for existing is that it does not
work that way — dividing two billion items into slots would be thirty-one million
of them.

The sample always has a count of one. It is the identity of what is stored —
item and components — and nothing else; how many there are is the other field.
Keeping the two apart is what stops a stack size from leaking into the total.

**The count is a long, and that costs nothing.** An item stack counts with
an int, but no item stack ever carries this number: what leaves a heap is a stack
of at most a stack, worked out from the total rather than being it. The total is
therefore free to be as wide as it likes, and the only care needed is at the three
places the outside world asks in ints, where the answer is clamped on the way out.

**This must never implement `net.minecraft.world.Container`.** A
hopper prefers the container path over the item handler when a block offers both,
and a container that tries to describe billions in slots of sixty-four is exactly
the shape that goes wrong. InfChest had to add a mixin to force hoppers off that
path; not being a container at all is the same fix, made earlier.

**`private static final String AMOUNT`**

The same name every heap uses for how much it holds.

It was `Count` here, `Amount` on two others and `Stored` on
the fourth — three names for one idea, which meant that `/data merge block`
worked on one heap and silently did nothing on the rest. The old name is still
read so that heaps saved before this keep their contents.

**`public ItemStack sample()`**

The identity of what is stored, with a count of one. Never handed out to be mutated.

**`public HeapItemHandler handler()`**

One handler for the whole block, handed to every side.

Not one per query. Two handler objects for one heap are two places a stale
answer could live, and telling two askers different things about the same
contents is how a storage block ends up creating items out of nothing.

**`public ItemStack stack()`**

One stack of what is inside, at most. What a person sees in the screen.

**`public ItemStack contents()`**

Everything inside, as one stack. What automation is shown, because an item
handler is allowed to report more than a stack and a heap that did not would be
telling every pipe and every storage network that it holds sixty-four.

**inside, at `shown.setCount((int) Math.min(Math.min(count, most), Integer.MAX_VALUE))`**

An item stack counts in an int however wide the total is.

**`public boolean accepts(ItemStack stack)`**

Whether this could go in. An empty heap accepts anything, which is what lets a
pipe or a hopper decide what it is for.

**`public boolean holds(ItemStack stack)`**

Whether this is *already* what the heap holds — a stricter question than
`accepts`, and the one a person should be asked.

The difference is the empty heap. Automation deciding what an empty heap is
for is the point of automation; a player who right-clicked to look inside and
silently committed the block to whatever happened to be in their hand has been
caught by it instead.

**`public int insert(ItemStack stack, boolean simulate)`**

Returns how many of the stack were taken, which may be none

**inside, at `int taken`**

The offered stack counts in an int, so what is taken always fits in one however wide the total is.

**`public ItemStack extract(int amount, boolean simulate)`**

Returns what was taken out: as much as was asked for, if there is that much.
        Not clamped to a stack — see `HeapItemHandler` for why not, and
        call it with a stack's worth when a stack is what is wanted.

**inside, at `sample`**

The sample is dropped with the last item: a heap that remembers what it used to hold would refuse the next thing put into it for no visible reason.

**inside, at `level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL)`**

The contents are drawn, so a client that does not hear about a change keeps drawing the old one.

**`protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)`**

The count is written even when it is zero, and that is not a formality.

An update packet carrying an empty tag is discarded before it reaches the
block entity — `IBlockEntityExtension#onDataPacket` guards on
`!tag.isEmpty()`. A heap that had just been emptied would therefore save
nothing, send nothing the client would accept, and go on being drawn holding
whatever it held a moment ago. Writing one field always is what makes emptying
a thing the client is told about.

**inside, at `count`**

getLong reads a tag that was written as an int too, so a heap saved before the total widened comes back without a migration step. The old key is read when the new one is absent, for the same reason.

**`public CompoundTag getUpdateTag(HolderLookup.Provider registries)`**

The client is sent the same fields, because it draws them.


**Contents the game cannot read**

When the saved kind cannot be parsed — the mod that adds it is gone — the heap keeps
the raw sample tag and its amount instead of reading the amount as zero, and writes
them back unchanged. Until the kind is readable again it takes nothing and gives
nothing on any path, and it cannot be locked or unlocked. Putting the mod back
restores it, because nothing about it was ever rewritten. The fluid heap and the
carried forms of both do the same.
## `HeapBlockItem.java`

**`public class HeapBlockItem extends ContentsBlockItem`**

The heap as an item.

Its whole reason to exist is the tooltip. A heap carries its contents when it
is picked up, so one sitting in an inventory can be holding two billion of
something and look exactly like an empty one. Saying what is inside is not a
nicety here; without it the item lies.

**`public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)`**

Sneak and right-click the air to look inside the one you are holding.

The air, because right-clicking a block is how a heap is placed and that must
keep working; `useOn` runs first and only a miss reaches here. Sneaking,
because a plain right-click with a heap in hand already means something on the
heap in front of you.

The screen it opens is the block's own — same slot, same numbers — with one
difference the screen states outright: nothing comes out. See `CarriedHeap`.

**`public java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> getTooltipImage(`**

The contents as a picture rather than as a line of text; see `HeapContents`.
Offered for anything at all, and the client decides there is nothing to draw when
the heap is empty.

**inside, at `CarriedHeap heap`**

Read through the same class the slot and the screen read through, rather than off the tag here: three readings of one field is three places for the key to be spelt differently, which is what happened to the count once already.

**inside, at `if (stack.getCount() == 1 && AcervusConfig.SPEC.isLoaded()`**

Said only while it is true, because it is a thing the item is quietly doing to items the player expected to end up in a slot. A heap that is full, or one in a game where the setting is off, says nothing.

## `HeapContents.java`

**`public record HeapContents(ItemStack of) implements TooltipComponent`**

"Draw what is in this" — a heap, or a rack of them.

A line of text says everything a picture does, and says it slower. A rack of
nine heaps says it nine times, at which point nobody reads any of it. So each
kind of contents gets its own small icon beside its name and its amount, and a rack
becomes a list that can be taken in without opening it.

**It carries the item itself and nothing worked out from it.** That is not
laziness: `Item#getTooltipImage` is handed a stack and nothing else, and
reading a heap needs the registries — which are reachable from the client, where
tooltips are drawn, and not from here. So the reading happens in
`io.github.capsicum0907.acervus.client.HeapContentsTooltip` along with the
drawing, and this stays the one thing it is safe for a class in every game to know:
which stack is being asked about.

## `HeapItemHandler.java`

**`public class HeapItemHandler implements IItemHandler`**

The window a heap shows to hoppers, pipes and anything else that moves items.

One slot. What is in it is what is in the heap, and taking from it takes as
much as was asked for.

<h2>Why this exceeds a stack, deliberately</h2>

The `IItemHandler` javadoc says two things about size, and only one of them
is kept here:
- `getStackInSlot` — "the result's stack size *may* be greater than
    the itemstack's max size". Kept, and the reason a heap says how much it really
    holds. Rounding it to a stack is not caution but a lie: an external storage
    reading one would report sixty-four of something there are a hundred thousand
    of, which is what InfChest does and what makes it read wrong in Refined
    Storage.
- `extractItem` — the result "must be less than or equal to `amount`
    *and* `getMaxStackSize()`". **Not kept.** Only `amount`
    bounds what comes out.

Breaking the second one is not an oversight. Every mod that moves large amounts
expects it broken: InfChest's own handler is `totalCount().min(amount)` with
no stack clamp, and that is precisely why a pipe with an unlimited upgrade can
empty one. Keeping the clause instead caps a heap at one stack per call — one
stack per tick against a pipe that asks once — and no amount of extra slots buys
that back honestly.

What it costs is that a caller which asks for more than a stack must be able to
hold what it gets. Callers that cannot ask for 64 and get 64; a caller that asks
for two billion has said it can take two billion. The guarantees this mod is
responsible for are the ones on the other side of the line, and those are kept
exactly: simulating never changes anything, the offered stack is never modified,
every side shares one handler, and nothing here remembers anything.

Both sides face every direction. A heap has no front.

**`public int getSlotLimit(int slot)`**

The capacity, so that `limit - count` is the room left.

## `HeapMenu.java`

**`public class HeapMenu extends AbstractContainerMenu`**

The screen's half on the server, for a heap in the world or a heap in a hand.

The heap is presented as two slots — `IntakeSlot` to put things in and
`HeapSlot` to take them out — rather than as buttons.
It is an item; it should be handled the way items are handled, with the clicks
everybody already knows. A button beside it would be a new thing to learn for
something the player can already do.

The count is not sent as menu data. Menu data fields are shorts on the wire,
which would cap what a heap can say at 32767. The block entity is already
synchronised to the client for drawing and the item is already synchronised as part
of the inventory, so in both cases the screen reads the heap where it lives.

What differs between the two is gathered in `Source`: where the heap is,
when the screen should close, and — for a heap in a hand — which of the player's own
slots must be frozen while its screen is open.

**`private static final int IN_X`**

Matches the slot positions in the generated screen texture.

**`public interface Source`**

Where a heap is kept, and what that means for the screen over it.

**`Pile pile()`**

Never null: a heap that has gone answers `Pile.NONE`.

**`default int frozen()`**

The hotbar slot that must not be moved while this screen is open, or -1.

Only a heap held in a hand has one, and it is the heap itself: moving it
out from under its own screen would leave the screen writing into a stack
that is somewhere else.

**inside, at `addSlot(new IntakeSlot(source::pile, IN_X, HEAP_Y))`**

Added unconditionally, even when the heap has gone. The slot indices below are counted from it, so a missing first slot would silently shift the range that shift-clicking moves things into.

**`public static HeapMenu at(int id, Inventory inventory, BlockPos pos)`**

The screen over a heap standing in the world.

**`public static HeapMenu inHand(int id, Inventory inventory, InteractionHand hand)`**

The screen over a heap being held, opened by sneaking and right-clicking the air.

**inside, at `if (!slot.mayPickup(player))`**

The frozen slot is the open heap itself. Shift-clicking it is the first thing anyone tries, and it must not put a heap inside itself.

**`private ItemStack outward(Pile heap)`**

Out of the heap and into the player. A stack at a time, and non-empty so that
the game asks again — which is how shift-clicking keeps going until either the
heap or the room runs out.

**inside, at `ItemStack taken`**

A stack's worth, explicitly: extraction is no longer clamped to one, so asking for everything here would empty the heap into a full inventory.

**beside `heap.insert(taken, false);`**

whatever would not fit goes back where it was

**`private ItemStack inward(Pile heap, Slot slot)`**

Into the heap, all of it at once: there is no reason to make this take turns.

**`private record AtBlock(Level level, BlockPos pos, ContainerLevelAccess access) implements Source`**

A heap standing in the world: valid while the player is near the block.

**`private record InHand(Player player, InteractionHand hand, CarriedHeap heap) implements Source`**

A heap being held: valid while that hand still holds one.

**`static final class Frozen extends Slot`**

The player's own slot holding the heap whose screen this is.

Refusing both directions is what covers the swap click as well — pressing a
number key over another slot checks this one before exchanging them.

## `HeapSlot.java`

**`public class HeapSlot extends Slot`**

A window onto a heap, shaped like a slot: the Out slot of the item heap's screen.

The point is that nothing new has to be learnt. Clicking takes a stack,
right-clicking takes half, shift-clicking moves as much as fits — all of that is the
game's own handling of a slot, and none of it is written here. Putting things in is
`IntakeSlot`'s job. What is written here is only the translation
between "a slot holding up to a stack" and "a heap holding billions".

That translation is one rule: **the slot shows a window, and setting it means
changing the total by the difference.** Every path the game has into a slot
eventually says "the slot now holds this"; reading that as a delta rather than as
an assignment is what makes paths nobody enumerated behave correctly anyway.

The two size limits are deliberately different, because the game asks them for
different reasons. Asked with no argument it is deciding how much to take out, and
the answer is a stack, so taking behaves as it does everywhere. Asked about a
particular stack it is deciding how much will fit, and the answer is the room the
heap has — a plain stack there is what makes a heap of five thousand look full.

The heap is fetched rather than held. A block can be broken and an item can
leave the hand while its screen is open, and either leaves `Pile.NONE` behind
— a heap that is not there answers exactly like an empty one, which is what stops
every method below from needing a null check.

**`private static final SimpleContainer UNUSED`**

The parent constructor needs a container; nothing ever reads it.

**`public boolean mayPickup(Player player)`**

A heap that does not give cannot be clicked out of, or shift-clicked out of.

**`public int getMaxStackSize()`**

How much comes out at once: a stack, whatever a stack of that item is.

**`public int getMaxStackSize(ItemStack stack)`**

How much will go in: everything the heap has room for.

**`public void set(ItemStack stack)`**

The delta rule. An empty stack means the caller believes the slot is now empty,
which it already is — the heap was changed by whatever emptied it.

**`public ItemStack safeInsert(ItemStack stack, int increment)`**

Written out rather than inherited, because the inherited version works out the
room as `getMaxStackSize(stack) - getItem().getCount()` and then assigns
the sum — which is the delta rule taking a longer path to the same place, and
one more place for the two limits above to be read in the wrong order.

**`public void onQuickCraft(ItemStack oldStack, ItemStack newStack)`**

Nothing to count: this slot is not part of any crafting.

## `Heaped.java`

**`public interface Heaped`**

What every heap has in common, which turned out to be less than it looked.

Four of them are written now, and the parts that are genuinely shared are: an
amount, a capacity, a name for what is being held, and a way of writing the amount
down. Everything else — how it is stored, what a "kind" even means, which API the
window speaks, whether it pushes or waits — differs at every one, and pulling those
together would have produced a base class made of branches.

So this is only what a readout needs. It exists because three of the four have
no items in them and therefore no slots to build a screen out of, and one screen
that can read any of them is worth more than three that each read one.

**`long amount()`**

How much is in it, in that resource's own unit.

**`long capacity()`**

How much would fit, in the same unit.

**`Component contentName()`**

What is being held — a fluid, a chemical — or empty when the resource has no kinds.

**`String brief(long value)`**

The short form of an amount, including any unit it should carry.

**`String exact(long value)`**

Every digit of an amount, for when the detail was asked for.

**`int tint()`**

The colour of the fill on a gauge, which is also the colour of the block's glass.

**`default boolean hasKinds()`**

Whether this resource comes in kinds.

Energy does not: there is no such thing as a kind of it, so there is nothing
to name and nothing to draw. The screen leaves out the box that would show it
rather than presenting an empty square that will never hold anything.

**`default net.minecraft.resources.ResourceLocation contentTexture()`**

A sprite standing for the contents, or null when the screen must work it out
another way. Chemicals carry their own icon; fluids only have one on the client,
so a fluid heap leaves this null and the screen looks it up there.

**`default int contentTint()`**

The colour that sprite is drawn in.

**`default boolean gives()`**

Whether anything may come out of it.

A heap in the world gives; a heap in a pocket does not. The same rule and the
same word as `Pile.gives()`, so that a reader meeting it on the second heap
does not have to learn it twice. See `Held`.

**`Heaped NONE`**

A heap that is not there: the block was broken while its screen was open, or the
item left the hand holding it. Answering with this rather than with null is what
lets the screen be written without a check at every reading.

## `Held.java`

**`public abstract class Held implements Heaped`**

A fluid, energy or gas heap that is being carried rather than placed.

The contents ride on the item — that is how a heap survives being mined — so a
heap in a pocket is a full heap with nothing reading it. This is the reading, in
the same terms a readout already speaks, so one screen serves both.

**It takes and does not give.** `gives()` is false, which is the same
rule the item heap follows for the same reason: reaching a trillion of anything
from an inventory slot, with no block to place and nothing to stand next to, ends
every reason to build a storage room. What a carried one can still do is
`draw` — empty a bucket, a battery or a tank into itself — because pouring
something in is the direction that costs nothing.

The stack is fetched afresh every time rather than held: the item can be moved,
swapped or dropped while a screen is open on it, and writing into a stack that is
no longer anywhere would lose whatever was written.

**`public final boolean gives()`**

Whether anything may come out, decided by *where the item is* rather than
by what it is.

A heap in a pocket does not give, and the reason was never the item — it was
that reaching a trillion of anything from an inventory slot, with nothing to
place and nowhere to stand, ends every reason to build a storage room. The price
is putting a block down. A heap slotted into a `HorreumBlockEntity` has had
that price paid, by the controller, so the same reading code answers the other
way. One field, set where the heap is found.

**`protected abstract BlockEntityType<?> type()`**

Which block this is the item of; the written contents have to name it.

**`protected final void write(CompoundTag tag, boolean empty)`**

Writes the contents back, dropping the component altogether once there is nothing
left, so that emptied heaps stack with the other empty ones instead of looking
different for carrying an empty tag.

**Through `setBlockEntityData`, never `CustomData.of`.** The
component is persisted with `CustomData.CODEC_WITH_ID`, which refuses a tag
with no `id` naming the block entity — and refuses it while the player's
inventory is being saved, which takes the world down. Nothing catches this
earlier: the network codec does not check, so a heap written by hand looks
perfectly well until the first autosave.

## `HeldEnergyHeap.java`

**`public final class HeldEnergyHeap extends Held`**

An energy heap being carried. See `Held` for the rule it follows.

**`private static final String LEGACY_STORED`**

What the amount used to be called here; read when the current name is absent.

**`public static HeldEnergyHeap of(HolderLookup.Provider registries, ItemStack stack)`**

A heap being carried: it takes and does not give.

**`public static HeldEnergyHeap inHand(Player player, InteractionHand hand)`**

The one in that hand, whatever it is at the moment of asking.

**`public static HeldEnergyHeap stored(HolderLookup.Provider registries, ItemStack stack)`**

A heap slotted into a controller, which is a placed block, so it gives.

**`public boolean hasKinds()`**

Energy has no kinds, so there is nothing to name and nothing to draw.

**`public int receive(int offered)`**

Returns how much was taken in, which is never more than was offered

**`public int give(int wanted)`**

Returns how much was handed out, which is nothing at all unless this heap
        `gives()` — a carried one never does

**inside, at `write(tag, left <= 0)`**

No sample to forget: for energy, emptiness is the amount and nothing else.

## `HeldFluidHeap.java`

**`public final class HeldFluidHeap extends Held`**

A fluid heap being carried. See `Held` for the rule it follows.

**`public static HeldFluidHeap of(HolderLookup.Provider registries, ItemStack stack)`**

A heap being carried: it takes and does not give.

**`public static HeldFluidHeap inHand(Player player, InteractionHand hand)`**

The one in that hand, whatever it is at the moment of asking.

**`public static HeldFluidHeap stored(HolderLookup.Provider registries, ItemStack stack)`**

A heap slotted into a controller, which is a placed block, so it gives.

**`public FluidStack sample()`**

Identity only: the fluid and its components, always with an amount of one.

**`public String brief(long value)`**

Buckets, because millibuckets cost three digits of every number for nothing.

**`public int insert(FluidStack stack, boolean simulate)`**

Returns how much of the offered fluid was taken, which may be none

**`public FluidStack extract(int wanted, boolean simulate)`**

Returns what was taken out, which is nothing at all unless this heap
        `gives()` — a carried one never does

**inside, at `tag.remove(SAMPLE)`**

Forgotten with the last drop, here as on the block: one that remembered would refuse the next thing poured in with nothing to say why.

## `HorreumBlock.java`

**`public class HorreumBlock extends BaseEntityBlock`**

The rack. Right-click it to put heaps in and take them out.

Nothing else: it has no gestures of its own, because everything it does is done
through the heaps inside it and they already know how. What the block adds is a
single place for a pipe to reach all of them.

**`public List<ItemStack> getDrops(BlockState state, LootParams.Builder params)`**

The heaps ride on the dropped rack, and for a harder reason than usual.

Spilling would drop nine heap items on the floor, which sounds harmless
until one remembers what a heap holds: nine stacks of two billion, in a pile
of entities that can be walked away from, burned, or picked up by the wrong
hopper. A heap is a thing you move, and so is a rack of them.

**`public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target,`**

Middle-clicking one gives back what is in it, the same as a heap.

## `HorreumBlockEntity.java`

**`public class HorreumBlockEntity extends BlockEntity`**

A rack of heaps, and one place to reach all of them.

A heap already carries its contents as an item, so a rack of heaps needs no
storage of its own: it is a row of slots holding heap items, and every window it
offers the outside world is a view over what is in those slots. Nothing is copied
in, nothing has to be kept in step, and pulling a heap out takes its contents with
it because they were never anywhere else.

**The heaps inside give.** A heap in a pocket does not, and the reason was
never the item — it was that the price of drawing from a heap is putting a block
down. This is that block. See `Held.gives()`.

Mixed on purpose: one rack holds item, fluid, energy and gas heaps side by side,
and offers the matching window for each kind. Sorting them into four different
racks would be four blocks and four sets of pipes to say one thing.

**This must never implement `net.minecraft.world.Container`.** The same
reason as `HeapBlockEntity`: a hopper prefers the container path, and what it
would find there is heap *items* — it would carry the heaps away rather than
their contents, which is a very expensive misunderstanding.

**`public static final int SLOTS`**

How many heaps a rack holds.

Not a setting. The screen is a picture with nine slots drawn on it, and a
number that could change would have to be a number the picture could draw.

**`public NonNullList<ItemStack> heaps()`**

The heap items themselves, to be read and written in place.

**`public HorreumItemHandler items()`**

One handler each, for the whole block; see `HeapBlockEntity.handler`.

**`public <T> T read(int slot, net.minecraft.world.item.Item kind,`**

The heap in that slot read as the kind asked for, or null when the slot holds
something else — an empty slot, or a heap of a different kind.

Every window is built out of this, which is why it takes the reader rather
than knowing about any of them: the rack does not care what a fluid is.

**`public <T> List<T> readAll(net.minecraft.world.item.Item kind,`**

Every slot holding a heap of that kind, in slot order.

**`public static NonNullList<ItemStack> readHeaps(ItemStack rack, HolderLookup.Provider registries)`**

The heaps written onto a rack item, and read back off one.

**Both, here, and nowhere else.** Four places were spelling the shape out
for themselves — the block entity, the tooltip text, the tooltip picture and the
carried rack's screen — and the carried one spelled it differently: it wrote the
item list as the whole component instead of nesting it under `Heaps`. One
write and the other three could no longer find anything, so taking one heap out
of a rack in your bag appeared to destroy the other eleven. They were still
there; nothing could read them.

The same mistake the count made once, when it was `Count` on one heap
and `Amount` on three. A shape known in more than one place is a shape that
will disagree with itself.

**`public static void writeHeaps(ItemStack rack, NonNullList<ItemStack> heaps,`**

Through `setBlockEntityData`, which names the block entity; see `Held.write`.

**`public static boolean isHeap(ItemStack stack)`**

Whether that item may go in a rack slot at all.

**`public void changed()`**

Something inside changed. The heaps are drawn in the screen and their amounts
are read from the items themselves, so a client that is not told keeps showing
what it last saw.

**inside, at `tag.put(HEAPS, saved(heaps, waiting, registries))`**

Written even when every slot is empty, for the reason given in HeapBlockEntity#saveAdditional: an empty update tag is discarded before it reaches the client, so emptying would never be heard about.

## `HorreumBlockItem.java`

**`public class HorreumBlockItem extends ContentsBlockItem`**

The rack as an item, and the reason it needs its own class is the same as every
heap's: it carries what is inside it, so a full one and an empty one look alike.

The text says how many heaps; the picture beneath it says what is in each of
them, one row apiece. Nine lines of prose would be a tooltip nobody reads past,
which is what the icons are for.

**`public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)`**

Sneak and right-click the air to look inside the one you are holding.

**`public java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> getTooltipImage(`**

The contents as a picture rather than as a line of text; see `HeapContents`.
Offered for anything at all, and the client decides there is nothing to draw when
the heap is empty.

## `HorreumEnergyHandler.java`

**`public class HorreumEnergyHandler implements IEnergyStorage`**

The window a cable sees onto a rack of energy heaps.

Unlike items and fluids, `IEnergyStorage` has no index at all: energy has
no kinds, so there is nothing for a slot number to distinguish. A rack of energy
heaps is therefore **one pool** — the totals are added up, what comes in fills
them in order and what goes out drains them in order.

Adding up is where the int shows: a rack of nine heaps can hold nine times
what one of them can say. As everywhere else, the answer is to saturate rather than
to wrap — two billion is a wrong answer, a negative number is a broken one.

**inside, at `taken +`**

Simulating must move nothing, so what a simulated fill would take is worked out from the room rather than by filling and putting it back.

**`private int saturated(java.util.function.ToLongFunction<HeldEnergyHeap> of)`**

Added up in a long, handed out as the largest int it will fit in.

## `HorreumFluidHandler.java`

**`public class HorreumFluidHandler implements IFluidHandler`**

The window a pipe or a pump sees onto a rack of fluid heaps.

**One tank per fluid heap actually in the rack, not one per rack slot.** That
is a reversal, and the interface is what settles it: `fill` and `drain`
take no tank index at all — the routing below is this class's own — so an index is
only ever used to *read* a tank, within the tick it was asked for. Nothing
holds one between ticks, so nothing is broken by the numbering changing when a heap
is put in or taken out.

Twelve fixed tanks was the cautious answer and it had a visible price: anything
that lists a block's tanks — Jade, most obviously — drew twelve bars for a rack with
one fluid heap in it, eleven of them saying Empty forever. The item side keeps its
fixed slots, because `IItemHandler` really does take an index when it inserts
and extracts. See `HorreumItemHandler`.

Filling routes to **a tank that already holds that fluid before an empty one**.
The other way round, a bucket of water would claim whichever empty heap came first
and the rack would fill up with half-used tanks of the same thing.

**`private List<HeldFluidHeap> tanks()`**

The fluid heaps in the rack, in slot order. Read fresh: the slots change.

**inside, at `int filled`**

Twice over the tanks: everything that already holds this fluid, and only then the empty ones. One pass would commit an empty heap while a matching one stood half full beside it.

**`public FluidStack drain(FluidStack resource, FluidAction action)`**

Draining by kind: only a heap holding that fluid answers.

**`public FluidStack drain(int maxDrain, FluidAction action)`**

Draining by amount alone: the first heap with anything in it answers, and the
whole of what comes out is one fluid. Mixing two heaps into one drain would mean
inventing a stack that is half lava, which no fluid stack can be.

## `HorreumItemHandler.java`

**`public class HorreumItemHandler implements IItemHandler`**

The window a hopper, a pipe or a storage network sees onto a rack of item heaps.

**One handler slot per rack slot, always nine, whatever is in them.** Slot
indices are the one thing a pipe remembers between ticks, so they must not move
when a heap is taken out or a fluid heap is put in beside it. A rack slot holding
anything but an item heap reads as an empty handler slot that refuses everything —
which is what an empty slot is.

Everything else is `HeapItemHandler` widened by one index, including the
two departures from the `IItemHandler` javadoc it documents: the true count is
reported, and extraction is bounded by what was asked for rather than by a stack.

**`private CarriedHeap at(int slot)`**

The heap in that slot, ready to give, or null if there is not one.

**inside, at `return heap.sample().copyWithCount((int) Math.min(heap.count(), Integer.MAX_VALUE))`**

The whole count, saturated at what a stack can carry. Rounding it down to sixty-four is what makes an external storage misreport a heap.

**inside, at `return taken >`**

The offered stack is never modified; only what is left over is handed back.

**`public int getSlotLimit(int slot)`**

How much would fit if the slot were empty, which is what a great many pipes work
the room out from — see `HeapItemHandler` for why answering with a stack
makes a heap look full.

## `HorreumMenu.java`

**`public class HorreumMenu extends AbstractContainerMenu`**

The rack's screen: twelve slots that take heaps, and nothing else.

There is no readout. Every heap item already says what is inside it on its own
tooltip — that line exists because a full heap and an empty one look identical —
so a rack that repeated it beside each slot would be saying the same thing twice
and could disagree with itself.

**`private static final int RACK_X`**

Matches the slot positions in the generated screen texture.

**`public interface Source`**

Where the rack is: standing in the world, or held in a hand.

The same shape as `HeapMenu.Source` and for the same reason - the
slots and the screen do not care which, so only the finding differs.

**`NonNullList<ItemStack> heaps()`**

The twelve slots, to read and write in place. Never null.

**`void changed()`**

Something inside changed and has to be written down.

**`default int frozen()`**

The hotbar slot frozen while this screen is open, or -1.

**`public static HorreumMenu at(int id, Inventory inventory, BlockPos pos)`**

The screen over a rack standing in the world.

**`public static HorreumMenu inHand(int id, Inventory inventory, InteractionHand hand)`**

The screen over a rack being carried, opened by sneaking and right-clicking the
air.

The heaps inside can be moved about; what is inside *them* stays out of
reach, because nothing carried offers a window onto its contents. Taking a heap
out of a bag is moving an item, not drawing from a store.

**`private record AtBlock(Level level, BlockPos pos, ContainerLevelAccess access,`**

A rack standing in the world: valid while the player is near it.

**`public NonNullList<ItemStack> heaps()`**

A list nobody else holds, for the moment between the block going and the
screen noticing. Writing into it changes nothing, which is exactly right.

**`private record InHand(Player player, InteractionHand hand,`**

A rack being carried: valid while that hand still holds one.

The heaps are read out of the item once and written back on every change. The
item is the thing that survives being dropped, so it is the copy that counts.

**`public void changed()`**

Through `setBlockEntityData`, which names the block entity in the tag -
see `Held.write`. Writing it any other way takes the world down at the
next autosave.

**`private class HeapRackSlot extends Slot`**

One slot of the rack, reading and writing the source's list directly.

**One heap per slot.** Two heaps in a slot are one set of components
between them, so filling "the" heap would fill both — the duplication a shulker
box avoids by not stacking, and the same rule a carried heap follows.

## `Mods.java`

**`public final class Mods`**

Which optional mods are here.

A class that **must never name anything belonging to them**, which is the
entire reason it exists rather than being a method on the thing it guards.

This was learnt the hard way. The check used to be `GasHeap.present()`,
which reads as exactly the right thing and cannot work: calling a static method
initialises the class it is on, and `GasHeap` holds a
`BlockCapability<IChemicalHandler, …>` in a static field. Asking "is Mekanism
here?" therefore loaded a Mekanism class to find out, and without Mekanism the mod
failed during construction with `NoClassDefFoundError`. **The guard cannot
live behind the door it is guarding.**

Every use is a short-circuit: `Mods.mekanism() && GasHeap.ITEM.get() == …`.
The right-hand side is only reached when the answer is yes, and a method body that
mentions a missing class is fine as long as it is never run.

**`public static boolean mekanism()`**

Whether the gas heap exists at all in this game.

**`public static boolean refinedStorage()`**

Whether there is a storage network here that can be told a heap's real total.

## `Pile.java`

**`public interface Pile`**

A heap of one kind of item, wherever it happens to be kept.

There are two places: a block in the world, and an item in an inventory. They
store the same thing and answer the same questions, and until now only the block
could be asked — the screen, the slot and the menu were all written against
`HeapBlockEntity` directly. This is that dependency named, so the same slot
and the same screen work over either.

It is deliberately not a common base class. The two differ entirely in how they
persist, how they reach the client and what may be taken out of them; what they
share is the arithmetic, and that is all this says.

**`ItemStack sample()`**

The identity of what is stored, with a count of one.

**`long count()`**

How many, which is wider than any stack can carry.

**`ItemStack stack()`**

One stack of it at most: what a slot can show.

**`default boolean gives()`**

Whether anything may come out of it.

A heap in the world gives; a heap in a pocket does not. Reaching two billion
of anything from an inventory slot, with no block to place and nothing to stand
next to, makes every other kind of storage pointless — so putting it down is the
price of drawing from it. See `CarriedHeap`.

**`Pile NONE`**

A heap that is not there: the block was broken while its screen was open, or the
item left the hand holding it.

Answering with this rather than with null is what lets the screen and the menu
be written without a null check at every reading — an empty heap and a missing
one look the same to a reader, and should.

## `ReadoutMenu.java`

**`public class ReadoutMenu extends AbstractContainerMenu`**

The screen behind a fluid, energy or gas heap, standing in the world or in a hand.

One menu for all three resources, because what they have in common is exactly
what a screen needs: an amount, a capacity, a name and two slots for containers,
one to empty and one to fill. The item heap keeps its own — there the contents *are* a slot, and that
is worth more than sharing this.

Nothing about the heap is sent through the menu. A block is already synchronised
for drawing and a held item is already synchronised as part of the inventory, so
the screen reads the heap where it lives — which also avoids menu data fields,
whose sixteen bits would cap a heap at 32767 of anything.

What differs between the two places is gathered in `Source`.

**`private static final int IN_X`**

Matches the slot positions in the generated screen texture.

**`public interface Source`**

Where the heap is kept, and what that means for the screen over it.

**`Heaped heap()`**

Never null: a heap that has gone answers `Heaped.NONE`.

**`Vessel vessel(Vessel.Flow flow)`**

Never null either; a heap that has gone gets a spare nothing ever looks at.

**`default int frozen()`**

The hotbar slot frozen while this screen is open, or -1. See `HeapMenu.Source`.

**`default void tick()`**

A moment of moving whatever is in the vessel.

Nothing for a block: it is ticking already, and doing it twice would move
things at twice the rate the block advertises. A held heap has no block entity
to tick, so the menu is the only clock it has.

**`default boolean lendsTheVessel()`**

Whether the vessels belong to the screen and must be handed back when it closes.

**inside, at `addSlot(new VesselSlot(source.vessel(Vessel.Flow.IN), IN_X, VESSEL_Y, source::vesselChanged))`**

Added unconditionally, even when the heap has gone: the slot indices below are counted from it, so a missing first slot would silently shift the range that shift-clicking moves things into.

**`public static ReadoutMenu at(int id, Inventory inventory, BlockPos pos)`**

The screen over a heap standing in the world.

**`public static ReadoutMenu inHand(int id, Inventory inventory, InteractionHand hand)`**

The screen over a heap being held, opened by sneaking and right-clicking the air.

**`public void broadcastChanges()`**

Called once a tick per viewer while the screen is open, which is what a held heap
uses as its clock. The vessel first, then the ordinary synchronising.

**`public void removed(Player player)`**

A held heap's vessel belongs to the screen, so what is in it comes back when the
screen closes. A block's does not — the block keeps it, and it is still there the
next time anyone opens it.

**`public ItemStack quickMoveStack(Player player, int index)`**

Shift-clicking moves an empty container into Out and any other into In, or back out of either.

**`private record AtBlock(Level level, BlockPos pos, ContainerLevelAccess access, Vessel spareIn, Vessel spareOut)`**

A heap standing in the world: valid while the player is near the block.

**`private record InHand(Player player, InteractionHand hand, Held held, Vessel in, Vessel out)`**

A heap being held: valid while that hand still holds one.

The vessels are the menu's own and last as long as the screen does. Saving one
onto the item would mean a container could be left inside a heap in a pocket,
which is a second kind of storage nobody asked for.

**`private static Held heldHeap(Player player, InteractionHand hand)`**

Which of the three the player is holding, or null.

The gas one is asked for last and through its own package, so that a game
without Mekanism never loads a class that mentions a chemical.

## `VesselSlot.java`

**`public class VesselSlot extends Slot`**

The slot over a heap's `Vessel`: a plain one-item slot, held somewhere other
than a container.

One item, not a stack. Filling one bucket of a stack of sixteen and leaving the
other fifteen empty is a question with no good answer, and refusing to take the
stack in the first place is a clearer one.

## `client/AcervusClient.java`

**`public final class AcervusClient`**

The client half: what draws the block, and what draws its screen.

**`public static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event)`**

What turns `HeapContents` into something drawn.

## `client/HeapContentsTooltip.java`

**`public class HeapContentsTooltip implements ClientTooltipComponent`**

Draws `HeapContents`: one row per heap, an icon then a name then an amount.

The icon is whatever stands for the contents where they are usually seen — the
item itself, or a fluid's or a chemical's own sprite off the block atlas. Energy has
no kinds, so its row has no icon and says so by being a number on its own.

The reading is done here rather than carried, because the registries a heap needs
to be read are reachable from the client and not from the item. See
`HeapContents`.

**`private static final int AMOUNT_COLOUR`**

The amount, set apart from the name so a column of them reads as a column.

**`public record Row(ItemStack item, FluidStack fluid, ResourceLocation sprite, boolean onAtlas,`**

One heap: something to draw, what it is called, and how much of it.

Public only so the gas package can build one — it is the one kind of contents
this class must not read for itself.

**`private static final ResourceLocation BOLT`**

Energy's own icon, and the one thing here that is not stitched onto the block
atlas — it belongs to a tooltip rather than to a block, so it is blitted straight.

**`public static boolean anything(ItemStack stack)`**

Whether there is anything worth drawing, so an empty one is never offered.

**`private static Row row(ItemStack heap, HolderLookup.Provider registries)`**

One heap read as a row, or null when there is nothing in it.

The gas heap is asked for last and behind `Mods.mekanism()`, so that a
game without Mekanism never reaches a class that mentions a chemical.

**`public void renderText(Font font, int x, int y, Matrix4f matrix,`**

The text is drawn with the icons rather than as tooltip lines of its own, so that
a name always sits beside the picture it belongs to. Tooltip text and tooltip
images are laid out separately, and a list of twelve would drift apart.

**inside, at `IClientFluidTypeExtensions look`**

A fluid's sprite is only reachable from the client, which is here.

## `client/HeapRenderer.java`

**`public class HeapRenderer implements BlockEntityRenderer<HeapBlockEntity>`**

What makes a heap worth looking at: it shows what it holds, and how many. The item
floats inside the glass facing the camera; the count is written flat on each open
side, since a label turned to the camera sinks into the block when seen at an angle.

**`private static final float ITEM_SCALE`**

Big enough to read from across a room, small enough to sit inside the block.

**`private static final float TEXT_SCALE`**

Text is authored at sixteen pixels; this brings it down to world scale.

**`private static final float TEXT_DROP`**

Below the item, clear of it at the scale above.

**inside, at `items.renderStatic(sample, ItemDisplayContext.GUI, packedLight, OverlayTexture.NO_OVERLAY,`**

GUI, not GROUND: the contents should read as a picture of the item rather than as an item lying on a surface inside a box.

**inside, at `String text`**

Short: this is read across a room, where a size is wanted and thirteen digits is a wall. The whole number lives in the screen.

**inside, at `pose.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE)`**

Negative Y: the font draws downward, and the pose is already turned to face the camera, which leaves its Y axis pointing the other way.

**`public int getViewDistance()`**

Contents are the reason to look at one, so they should be visible from further than a sign.

## `client/HeapScreen.java`

**`public class HeapScreen extends ReadoutPanel<HeapMenu>`**

The item heap's screen, laid out by `ReadoutPanel` like the other three.

Over the Out slot the exact count is *added* to the item's own tooltip
rather than replacing it — what is in there is still an item, and everything an
item usually says about itself still applies.

## `client/HorreumScreen.java`

**`public class HorreumScreen extends AbstractContainerScreen<HorreumMenu>`**

The rack from the inside: twelve slots and the player's own inventory.

Deliberately plain. Each heap says what it holds on its own tooltip, so there is
nothing this screen could add that would not be a second copy of the same numbers.

Every measurement here has a twin in `tools/make_textures.py`.

**`public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)`**

See `HeapScreen.render`: the screen has to call this itself.

## `client/ReadoutScreen.java`

**`public class ReadoutScreen extends ReadoutPanel<ReadoutMenu>`**

The screen for the fluid, energy and gas heaps; the layout is `ReadoutPanel`'s.

A chemical carries its icon and its tint on itself. A fluid's are only reachable
from the client, so the fluid heap leaves them unanswered and they are looked up
here instead — which it can do because the fluid heap, unlike the gas one, exists in
every game.

## `data/AcervusDataGen.java`

**`public final class AcervusDataGen`**

Everything under `src/generated/resources` comes from here, so nothing in
that directory is written by hand.

No loot table: `io.github.capsicum0907.acervus.HeapBlock.getDrops` answers
that question in code, because what a heap drops depends on what is inside it.

**`private static class Tags extends BlockTagsProvider`**

A pickaxe is what breaks a heap quickly. Only quickly — the block deliberately
does not require a correct tool, because failing to drop would mean losing
everything inside it.

**`protected void addTags(HolderLookup.Provider registries)`**

The gas heap goes in as an **optional** entry, always, whether Mekanism is
here at datagen time or not.

A required entry naming a block that does not exist does not merely go
missing: the whole tag file is refused, and all four blocks fall out of
`mineable/pickaxe` together. So a game without Mekanism had a heap that
a pickaxe was no quicker at than a fist — which is what
`neverVoidsItselfForWantOfAPickaxe` caught the moment the mods were
taken out of the folder.

Writing it optionally and unconditionally also ends the older trap that the
generated files depended on what happened to be in `run/mods`.

**inside, at `String rack`**

Solid: a rack holds heaps rather than contents, so there is nothing to see through and no reason to pay for translucency.

**inside, at `if (Mods.mekanism())`**

Only when Mekanism is present, because the block only exists then. Keep Mekanism in run/mods when regenerating, or these assets go stale.

**`private void drawnItem(DeferredBlock<?> block)`**

translucent, because the middle of the texture is see-through and the
default render type would draw those pixels as fully opaque.

**inside, at `add(AcervusRegistry.HEAP.get(), "Item Heap")`**

"Item Heap", not "Heap": it is one of four, and being the first written is not a reason for it to be the one without a surname.

**inside, at `add(AcervusRegistry.HORREUM.get(), "Horreum")`**

Latin for a granary: the building heaps are kept in.

**inside, at `add("gui.acervus.empty", "Empty")`**

Shared by all four: emptiness is not a fact about any one of them.

**`private static class Recipes extends RecipeProvider`**

Every heap shares one frame — iron blocks at the corners, nether stars above and below,
netherite ingots at the sides — around the piece that says what it holds: a chest, a
cauldron, a redstone block or Mekanism's block of osmium, which its gas tanks are made of. Heaps are meant to be built in numbers, so
every ingredient stacks; nothing in it is a one-off. Iron and nether stars can be
farmed, and netherite is the one that has to be dug. Glass is not in it: the windows are how three of
the heaps look, not something all four share.

**`private static void rack(RecipeOutput output, Item heap)`**

A rack is netherite ingots at the corners and nether stars at the sides around a
heap. Only an empty heap will do. Crafting uses the ingredient up, and a heap with
something in it would take everything it holds with it. There is one recipe per kind
of heap, so that the gas heap's can carry the condition that the gas heap exists.

**inside, at `if (Mods.mekanism())`**

A block of osmium rather than anything vanilla: the same ingredients cannot make two different blocks, and osmium is what Mekanism's own gas tanks are made of. Two different questions, asked at two different times, and both have to be answered or the recipe is wrong in one direction or the other: - The guard: is Mekanism here NOW, while this runs? Naming the block at all loads the class that holds it, and that class cannot exist without Mekanism. So the file is only written when datagen is run with Mekanism in run/mods -- and, because of --all, a datagen run WITHOUT it would delete the committed file rather than leave it alone. - The condition: does the item exist in the game the recipe is being loaded into? The file ships in the jar always, Mekanism or not, and without it the recipe names an item nobody registered. That is not silently skipped: it is an ERROR in the log every launch. The condition asks after our own item rather than after Mekanism, for the same reason the pickaxe tag uses addOptional -- the thing that must exist is ours, and naming somebody else's mod would only be a guess at why.

## `data/TestStructures.java`

**`public class TestStructures implements DataProvider`**

The stage the game tests run on: a flat floor with clear air above it.

A game test needs a structure to be placed in, and there is no empty one to
borrow. Writing the NBT here rather than checking a binary into the repository
keeps the rule that generated files are generated, and the data version comes
from the game itself so it cannot drift out of date silently.

**`public static final String FLOOR`**

Referenced by `@GameTest(template = ...)`.

**inside, at `ListTag blocks`**

Every cell is listed, air included: an omitted cell is left as whatever was already there, which would let one test leave something behind for the next.

**beside `@SuppressWarnings("deprecation")`**

Hashing.sha1 is what CachedOutput expects

## `gas/Chemistry.java`

**`public interface Chemistry`**

The gas heap is registered in every game, so that taking Mekanism out of a world does
not take the blocks with it. Nothing that is always loaded may name a Mekanism type,
because the JVM can go looking for a type it sees in a class being loaded, not only
one that is called. So the heap keeps what it holds as Mekanism wrote it — the saved
chemical as a tag, and an amount — and compares kinds by their id.

Everything that needs Mekanism to understand the tag goes through this interface:
whether it can be read, its name, icon and tint, whether a container is empty, and
moving chemicals between a heap and the containers in its slots. With Mekanism it is
`MekanismChemistry`, in `gas.mek`, which is only ever loaded when Mekanism is present
and also registers the chemical handlers for pipes. Without it, `NONE` reads nothing,
so every gas heap's contents are unreadable: kept, shown as the missing texture, and
never moved.

The recipe asks for any block in `c:storage_blocks/osmium` rather than for Mekanism's
own item by id, so that the file can be written without Mekanism, and it carries the
condition that Mekanism is loaded.

## `gas/ChemicalHeapBlock.java`

**`public class ChemicalHeapBlock extends BaseEntityBlock`**

The chemical heap. No interaction of its own: gas moves through pressurised tubes,
and there is no held container to right-click one with.

**`public <T extends net.minecraft.world.level.block.entity.BlockEntity>`**

Server side only.

**`protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,`**

Empty-handed: open the readout.

## `gas/ChemicalHeapBlockEntity.java`

**`public class ChemicalHeapBlockEntity extends BlockEntity implements io.github.capsicum0907.acervus.Heaped, io.github.capsicum0907.acervus.HasVessel`**

What a chemical heap holds: one chemical, and how much of it.

**The only one of the four that can tell the whole truth.** Mekanism counts
chemicals in longs — `getChemicalTankCapacity` returns one, and so does the
amount on a stack — so nothing here has to saturate. An item heap, a fluid heap
and an energy heap all hold a long internally and then say the largest int they
can; this one says what it holds.

Everything in this package exists only when Mekanism does, which is why it is
in a package of its own: nothing outside it mentions a chemical, so a game without
Mekanism never loads a class that would be missing one.

**`private ChemicalStack sample`**

Identity only: the chemical, always with an amount of one.

**`public ChemicalStack contents()`**

All of it, with no clamp anywhere: the amount is a long at both ends.

**`public long insert(ChemicalStack stack, boolean simulate)`**

Returns how much of the offer was taken

**`public ChemicalStack extract(long wanted, boolean simulate)`**

Returns what was taken out: as much as was asked for, if there is that much

## `gas/ChemicalHeapBlockItem.java`

**`public class ChemicalHeapBlockItem extends ContentsBlockItem`**

The gas heap as an item, saying what it carries so that it is not lying.

**`public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)`**

Sneak and right-click the air to look inside the one you are holding.

**`public java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> getTooltipImage(`**

The contents as a picture rather than as a line of text; see `HeapContents`.
Offered for anything at all, and the client decides there is nothing to draw when
the heap is empty.

## `gas/ChemicalHeapHandler.java`

**`public class ChemicalHeapHandler implements IChemicalHandler`**

The window a chemical heap shows to Mekanism's pipes and machines.

The plainest of the four, because Mekanism counts in longs. There is no
saturating, no clamping and no choosing which of two true things to say: the tank
reports what is in it and hands over what is asked for, and both numbers are the
real ones. The other three heaps are all shaped around a ceiling that this one
does not have.

Nothing is remembered here, for the same reason as the others.

**`public void setChemicalInTank(int tank, ChemicalStack stack)`**

Assignment, which a heap has no natural meaning for. Read as "hold exactly this
instead" — the only reading that leaves the block consistent afterwards.

## `gas/GasHeap.java`

**`public final class GasHeap`**

The gas heap, and the whole of what Acervus knows about Mekanism.

Everything here is registered only when Mekanism is installed. That is not
caution about the dependency being optional — it is what makes it optional: a
block whose block entity holds a `ChemicalStack` cannot be registered
without the class that defines one, so the block has to not exist instead.

The capability is built by name rather than read off Mekanism's own class.
Capabilities are interned by name and type, so
`mekanism:chemical_handler` resolves to the very object Mekanism registers,
and this mod compiles against the published API alone rather than against the
mod's internals.

**`public static final BlockCapability<IChemicalHandler, Direction> CHEMICAL_HANDLER`**

The same capability object Mekanism registers, reached without its class.

**`public static final net.neoforged.neoforge.capabilities.ItemCapability<IChemicalHandler, Void>`**

The item-level twin of the same capability, for a tank held in the hand.

**`public static void register(IEventBus modEventBus)`**

Loading this class is what registers everything in it, so the caller has to have
checked first — and the check cannot live in here, because *calling* it
would be what loads the class.

That is not a nicety. The check used to be a static method on this class, and
a static method initialises the class it is on: asking "is Mekanism here?" loaded
a Mekanism class to find out, and without Mekanism the mod died during
construction. It lives in `io.github.capsicum0907.acervus.Mods` now.

**`public static void registerRackCapability(RegisterCapabilitiesEvent event)`**

The rack's chemical window, registered from this side of the fence.

The rack itself lives in the main package and exists in every game, so it
cannot name a chemical handler. It calls in here instead, and only after asking
`io.github.capsicum0907.acervus.Mods.mekanism()` — which is why this is a method
of its own rather than part of
`registerCapability`.

**beside `@SuppressWarnings("DataFlowIssue")`**

the vanilla builder wants a data fixer type it never uses

## `gas/GasRow.java`

**`public final class GasRow`**

A gas heap read as a tooltip row.

It is here rather than beside the other three because it is the only one that
names a chemical, and nothing outside this package may. The caller reaches it only
after `Mods.mekanism()`, so a game without Mekanism never loads it.

A chemical carries its own icon and its own tint, unlike a fluid, so the sprite
is settled here and the drawing side has nothing to look up.

**`public static HeapContentsTooltip.Row of(HolderLookup.Provider registries, ItemStack heap)`**

Null when the heap is empty, as with every other kind.

## `gas/HeldChemicalHeap.java`

**`public final class HeldChemicalHeap extends Held`**

A gas heap being carried. See `Held` for the rule it follows.

The one of the four with no ceiling anywhere: Mekanism counts in longs at both
ends, so nothing here saturates on the way out.

**`public static HeldChemicalHeap of(HolderLookup.Provider registries, ItemStack stack)`**

A heap being carried: it takes and does not give.

**`public static HeldChemicalHeap inHand(Player player, InteractionHand hand)`**

The one in that hand, whatever it is at the moment of asking.

**`public static HeldChemicalHeap stored(HolderLookup.Provider registries, ItemStack stack)`**

A heap slotted into a controller, which is a placed block, so it gives.

**`public ResourceLocation contentTexture()`**

A chemical carries its own icon and its own colour, unlike a fluid.

**`public long insert(ChemicalStack stack, boolean simulate)`**

Returns how much was taken, which may be none

**`public ChemicalStack extract(long wanted, boolean simulate)`**

Returns what was taken out, which is nothing at all unless this heap
        `gives()` — a carried one never does. No clamp anywhere: Mekanism
        counts in longs at both ends.

**inside, at `tag.remove(SAMPLE)`**

Forgotten with the last of it, here as on the block.

## `gas/HorreumChemicalHandler.java`

**`public class HorreumChemicalHandler implements IChemicalHandler`**

The window Mekanism's tubes see onto a rack of gas heaps.

**One tank per gas heap actually in the rack, not one per rack slot.** The
same reversal as `io.github.capsicum0907.acervus.HorreumFluidHandler`, and for
the same reason: twelve fixed tanks meant twelve bars in anything that lists them,
eleven of which said Empty forever.

This one is the less clear-cut of the two, because `insertChemical` and
`extractChemical` *do* take an index where the fluid interface does not.
What makes it safe is that Mekanism reaches them through the handler's own sideless
defaults, which walk the tanks in the tick they were asked for; nothing carries a
tank number from one tick to the next. If something ever does, the symptom is an
insert landing in the wrong heap of the same rack — not a loss, and not a
duplication.

The one that needs no care at the edge. Mekanism counts in longs at both ends,
so nothing here saturates, and the tank reports exactly what it holds.

This class exists only when Mekanism does. It is in this package for that
reason, and nothing outside the package names a chemical.

**`private java.util.List<HeldChemicalHeap> tanks()`**

The gas heaps in the rack, in slot order. Read fresh: the slots change.

**`public void setChemicalInTank(int tank, ChemicalStack stack)`**

Assignment, read as "hold exactly this instead"; see `ChemicalHeapHandler`.

## `rs/HeapStorage.java`

**`public class HeapStorage implements ExternalStorageProvider`**

What a Refined Storage network sees when its external storage faces a heap or a
rack of them.

**This exists for one number.** Refined Storage counts in longs everywhere —
`ResourceAmount` carries one, `insert` and `extract` take one —
and a heap holds a long. The two agree perfectly, and until now they had to speak
through `IItemHandler`, whose `ItemStack` counts in an int. So a network
looking at a heap of five billion was told 2,147,483,647 and believed it. Nothing
about Refined Storage was the limit; the adapter between them was.

Moving is still done in int-sized calls, because that is all a stack can carry,
and a network that wants more simply asks again. It is the *reading* that had
no way to be honest, and now does.

**Always non-null, even when the block is not ours.** Refined Storage collects
providers with `.map(factory -> factory.create(…)).toList()` and does not
filter what comes back, so a factory that returned null for somebody else's block
would put a null in that list. One that answers "nothing here" is the shape the
interface actually asks for.

**`private List<Pile> piles()`**

Every heap behind this face, read fresh.

A single heap is one entry; a rack is one per item heap in it. Looked up each
time rather than remembered, because the block can be broken, replaced, or have
its heaps taken out while the network is still pointing at it.

**inside, at `amounts.add(new ResourceAmount(ItemResource.ofItemStack(pile.sample()), pile.count()))`**

The whole count, as a long, which is the entire point of this class.

**`private static int atMostAnInt(long amount)`**

One call moves at most what an `ItemStack` can *count* — two billion,
not sixty-four. `ItemResource.toItemStack(long)` casts rather than clamping
to a stack, so the only real ceiling is the int, and clamping first is also what
keeps Refined Storage from logging a truncation warning about it.

A network wanting more than two billion in one call asks again. Nothing is
lost by saying "this much for now"; it was the *reading* that had no way to
be honest.

**`private interface Pile`**

A heap, wherever it is kept. The two places differ only in who to tell.

**`private record Racked(HorreumBlockEntity rack, CarriedHeap heap) implements Pile`**

A heap in a rack, which has to be told that one of its items changed.

## `rs/RefinedStorage.java`

**`public final class RefinedStorage`**

The whole of what Acervus knows about Refined Storage: one registration.

Everything here is loaded only when Refined Storage is installed, and the check
lives in `Mods` rather than in this class — see
`io.github.capsicum0907.acervus.gas.GasHeap.register` for the crash that rule
was bought with.

Registered in common setup rather than in the mod constructor, because
`RefinedStorageApi.INSTANCE` is something Refined Storage fills in during its
own construction, and mod constructors run in an order nobody should rely on.

**inside, at `event.enqueueWork(() -> RefinedStorageApi.INSTANCE.addExternalStorageProviderFactory(`**

The factory is asked about every external storage in the world, ours or not, and must answer with something either way. HeapStorage answers "nothing here" when the block is somebody else's.

## `tools/make_textures.py`

**the script**

Draw the heap block texture.

This script is the source of the sprite; the PNG under src/main/resources is its
output and is not edited by hand.

The block has to be seen into, so the middle of the face is translucent and only
the frame is solid. Shading is derived rather than drawn: a pixel with nothing
above-left of it catches the light, a pixel with nothing below-right of it falls
into shadow, and everything else is body colour. The frame is a ring, so that rule
lights its outer edge and shades its inner one without either being described.

No third-party libraries: the PNG is assembled from zlib and struct.

    python tools/make_textures.py

**`draw`**

The same block in a different glass. The frame is shared on purpose: a fluid
heap and an item heap are the same machine holding different things, and should
look like two of a set rather than two inventions.

**`_recess`**

A sunken area: dark along the top and left, light along the bottom and right.

**`_sheet`**

A 256x256 sheet, transparent wherever nothing was drawn. The three screens
share this rather than each carrying its own copy of the PNG writing.

**`draw_bolt`**

Sixteen by sixteen, transparent but for the bolt. Blitted straight rather than
stitched onto the block atlas: it belongs to a tooltip, not to a block.

The drawing is centred by measuring it, not by counting dots in the art above.
Written out by hand it sat against the top-left corner of its frame while every
other icon in the row was centred, and the fix for that is not to move the dots -
it is to stop the position being something anyone has to get right.

**`draw_rack_screen`**

Nine slots and the player's inventory, and nothing else. Each heap says what
it holds on its own tooltip, so a readout here would be a second copy of it.

**beside `FRAME = 2`**

how many pixels deep the metal border runs

**beside `GLASS_ALPHA = 90`**

out of 255; enough to read as glass, clear enough to see through

**beside `raw.append(0)`**

filter type 0 for the row

**beside `header = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)`**

8-bit RGBA

**beside `INVENTORY_ROWS = ((8, 84), (8, 102), (8, 120))`**

inner top-left of each row

**`SHEEN = {(4, 5), (5, 4), (5, 5), (6, 4), (9, 10), (10, 9), (10, 10)}`**

A few brighter cells, as the sheen on a pane. Placed rather than computed: a highlight is about where the light happens to be, not about the shape.

**`METAL_TONES = ("#8A8F9C", "#5C6270", "#3A3F4A")`**

(light, body, shadow)

**`SHEET = 256`**

--- the screen ------------------------------------------------------------ A panel in the game's own idiom: flat fill, a light bevel on the top and left, a dark one on the bottom and right. Every measurement below is also a constant in HeapScreen, and the two have to agree; they are named the same on both sides.

**`FLUID_GLASS = "#6A9CE0"`**

One frame, three glasses. The colour is the only thing that says which resource a heap is for, which is the intent: they are the same machine.

**`BOLT = """`**

Energy has no item and no sprite of its own, so its tooltip row had a hole where every other row has a picture. Drawn rather than described, because the shape is the point: nobody needs to be told what a lightning bolt means.

**`edge = (x - 1, y) not in lit or (x, y - 1) not in lit`**

An edge is any lit pixel with an unlit neighbour above or to the left, which gives the bolt a rim without anyone placing one.

**`RACK_SLOTS = (62, 18)`**

Where the nine heap slots sit in the rack screen: three across, three down.
## Build and CI

**`build.gradle`: repositories**

ModMaven is there for Mekanism's chemical API and nothing else, and is scoped to the
`mekanism` group so that nothing can start resolving from it by accident.

Refined Storage publishes no API artifact anywhere — not to Central, not to ModMaven.
Its own jar off Modrinth is the only way to compile against it, and it is pinned to a
version so the build stays reproducible. That repository is scoped to its one group
too.

**`build.gradle`: dependencies**

Mekanism's chemical API is a compile-time dependency only. Mekanism itself is never
required: the gas heap is registered only when it is present, and nothing else in the
mod mentions it.

Refined Storage is likewise compile time only. What Acervus wants from it is one
registration point: an external storage provider reads a heap in longs, which is what
an item handler cannot carry.

`gradle.properties` pins both versions (`mekanism_version`, `refinedstorage_version`).

**`build.gradle`: run configurations**

A forked JVM is not attached to the console, so on a Japanese Windows it falls back to
CP932 while the console reads UTF-8. `run.bat` lines up the console; the
`file.encoding`, `stdout.encoding` and `stderr.encoding` properties line up the writer.

**`.github/workflows/build.yml`**

Documentation-only pushes do not change what the jar does, and the game tests are the
expensive half of the workflow, so pushes that touch only `**.md`, `LICENSE` or
`.gitignore` are skipped. `workflow_dispatch` takes no path filter by design: a manual
run is asked for by someone who already knows what they want checked, and it is the
way to re-verify a branch nothing has been pushed to — the toolchain and the
dependencies move even when the code does not.

Only the newest push to a branch is worth checking. Without `cancel-in-progress`, a
run of small commits leaves a queue of jobs each verifying a state nobody will keep.

Compiling only proves the code is well formed. The game test step is what checks the
mod does what it claims: it launches a headless server, runs every registered test,
and fails if any of them fail. It looks for `@GameTestHolder` first rather than
assuming tests exist, because GameTestServer treats having no tests as a reason to
crash; the skip is announced so that "no tests ran" cannot be read as "all tests
passed". The server also exits zero when the mod fails to construct, so the step
requires the `GAME TESTS COMPLETE` summary line as proof the tests were reached.
