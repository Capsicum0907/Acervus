package io.github.capsicum0907.acervus;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.github.capsicum0907.acervus.data.TestStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.stats.Stat;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * What a heap holds, and — the part that actually breaks in blocks like this — what
 * it lets out through the item handler that hoppers and pipes talk to.
 *
 * <p>Run with {@code gradlew runGameTestServer}.
 */
@GameTestHolder(Acervus.MODID)
@PrefixGameTestTemplate(false)
public final class AcervusTests {
    private static final BlockPos WHERE = new BlockPos(2, 1, 2);

    /** A second heap, to pipe the first one into. */
    private static final BlockPos OTHER = new BlockPos(2, 1, 4);

    /** Far past a stack, and past a shulker box, so nothing accidental can pass. */
    private static final int MANY = 5_000;

    /** The first hotbar slot in a heap menu: the heap's own slot, then three rows. */
    private static final int HOTBAR_FIRST = 1 + 27;

    private AcervusTests() {
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void holdsFarMoreThanAStack(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);

        int taken = heap.insert(new ItemStack(Items.DIAMOND, MANY), false);

        check(taken == MANY, "a heap should have taken all " + MANY + ", not " + taken);
        check(heap.count() == MANY, "a heap should be holding " + MANY + ", not " + heap.count());
        helper.succeed();
    }

    /**
     * A heap says how much it really holds, and gives out as much as it is asked for.
     *
     * <p>Both halves are what makes the ecosystem work, and one of them is a
     * deliberate departure from the {@code IItemHandler} javadoc. Reporting the true
     * count is explicitly allowed and is the difference between an external storage
     * reading a hundred thousand and reading sixty-four. Handing out more than a
     * stack is <em>not</em> allowed by the written contract and is what every mod
     * that moves large amounts relies on anyway — InfChest's own handler is
     * {@code totalCount().min(amount)} with no stack clamp, which is why a pipe with
     * an unlimited upgrade can empty one.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void tellsTheTruthAndGivesWhatIsAsked(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);
        heap.insert(new ItemStack(Items.DIAMOND, MANY), false);
        IItemHandler handler = handler(helper, WHERE, null);

        check(handler.getStackInSlot(0).getCount() == MANY,
                "a heap should report all " + MANY + ", not " + handler.getStackInSlot(0).getCount());

        ItemStack out = handler.extractItem(0, MANY, false);
        check(out.getCount() == MANY, "and should hand over all " + MANY + ", not " + out.getCount());
        check(heap.isEmpty(), "leaving nothing behind");
        helper.succeed();
    }

    /** Asking for a stack still gets exactly a stack: the amount is what bounds it. */
    @GameTest(template = TestStructures.FLOOR)
    public static void givesNoMoreThanWasAskedFor(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);
        heap.insert(new ItemStack(Items.DIAMOND, MANY), false);

        ItemStack out = handler(helper, WHERE, null).extractItem(0, 64, false);

        check(out.getCount() == 64, "asking for 64 should give 64, not " + out.getCount());
        check(heap.count() == MANY - 64, "and take exactly that many out of the heap");
        helper.succeed();
    }

    /** The path a hopper and every mod's pipes actually take. */
    @GameTest(template = TestStructures.FLOOR)
    public static void fillsAndDrainsThroughTheHandler(GameTestHelper helper) {
        place(helper);
        IItemHandler handler = handler(helper, WHERE, null);

        ItemStack left = handler.insertItem(0, new ItemStack(Items.DIAMOND, 64), false);
        check(left.isEmpty(), "the handler should have taken the whole stack");

        ItemStack out = handler.extractItem(0, 64, false);
        check(out.getCount() == 64, "the handler should have given back 64, not " + out.getCount());
        check(out.is(Items.DIAMOND), "the handler should have given back what went in");
        helper.succeed();
    }

    /**
     * A great many pipes work out how much fits as {@code limit - count} instead of
     * asking. If the only slot showed a full stack, a heap holding a thousand would
     * look full to them and quietly stop accepting.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void staysOpenToPipesWhenPastAStack(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);
        heap.insert(new ItemStack(Items.DIAMOND, MANY), false);
        IItemHandler handler = handler(helper, WHERE, null);

        int space = handler.getSlotLimit(0)
                - handler.getStackInSlot(0).getCount();
        check(space > 64, "a heap with room left should show it, not " + space);
        check(handler.insertItem(0, new ItemStack(Items.DIAMOND, 64), false).isEmpty(),
                "and should still take a stack");
        helper.succeed();
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void refusesASecondKind(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);
        heap.insert(new ItemStack(Items.DIAMOND, 10), false);

        check(heap.insert(new ItemStack(Items.GOLD_INGOT, 10), false) == 0,
                "a heap holding diamonds should refuse gold");
        check(!handler(helper, WHERE, null).isItemValid(0, new ItemStack(Items.GOLD_INGOT)),
                "and should say so before being asked to take it");
        helper.succeed();
    }

    /**
     * Nothing is created and nothing is lost, however many sides are working at once.
     *
     * <p>Storage blocks of this kind duplicate for three reasons, and this pins all
     * three: a simulated answer that differs from the real one, an insert that
     * quietly modifies the stack it was handed — so the caller keeps it *and* the
     * heap gains it — and per-side handlers that each remember their own version of
     * the contents. Every side is asked for its handler separately here, and the
     * total is checked against what went in.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void createsNothingUnderInterleavedAccess(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);
        IItemHandler[] sides = new IItemHandler[Direction.values().length];
        for (Direction side : Direction.values()) {
            sides[side.ordinal()] = handler(helper, WHERE, side);
        }

        int put = 0;
        int got = 0;
        for (int round = 0; round < 200; round++) {
            IItemHandler in = sides[round % sides.length];
            IItemHandler out = sides[(round * 5 + 3) % sides.length];

            ItemStack offer = new ItemStack(Items.DIAMOND, 64);
            int refusedIfAsked = in.insertItem(0, offer, true).getCount();
            int refused = in.insertItem(0, offer, false).getCount();
            check(refused == refusedIfAsked, "simulating an insert must not change its answer");
            check(offer.getCount() == 64, "inserting must not shrink the stack it was handed");
            put += 64 - refused;

            int peeked = out.extractItem(0, 32, true).getCount();
            int taken = out.extractItem(0, 32, false).getCount();
            check(taken == peeked, "simulating an extract must not change its answer");
            got += taken;
        }

        check(heap.count() == put - got,
                "the heap should hold " + (put - got) + " after " + put + " in and " + got
                        + " out, but holds " + heap.count());
        helper.succeed();
    }

    /**
     * An emptied heap forgets what it held. One that remembered would refuse the next
     * thing put into it, with nothing on the block to say why.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void emptyForgetsItsKind(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);
        heap.insert(new ItemStack(Items.DIAMOND, 10), false);
        heap.extract(10, false);

        check(heap.isEmpty(), "a heap that gave back everything should be empty");
        check(heap.insert(new ItemStack(Items.GOLD_INGOT, 10), false) == 10,
                "and should then take a different kind");
        helper.succeed();
    }

    /**
     * Breaking a heap must not spill what is inside: at capacity that would be tens of
     * millions of item entities. What it holds rides on the dropped block instead.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void carriesItsContentsWhenBroken(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);
        heap.insert(new ItemStack(Items.DIAMOND, MANY), false);

        BlockPos pos = helper.absolutePos(WHERE);
        List<ItemStack> drops = helper.getLevel().getBlockState(pos).getDrops(
                new LootParams.Builder(helper.getLevel())
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                        .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                        .withOptionalParameter(LootContextParams.BLOCK_ENTITY, heap));

        check(drops.size() == 1, "a heap should drop exactly one thing, not " + drops.size());
        check(drops.get(0).has(DataComponents.BLOCK_ENTITY_DATA),
                "the dropped heap should be carrying its contents");
        helper.succeed();
    }

    /**
     * An emptied heap has to have something to say. An update packet carrying an empty
     * tag is thrown away before the block entity sees it, so a heap that wrote nothing
     * when empty would keep being drawn holding what it no longer holds — which is
     * exactly what it did until this was pinned.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void tellsTheClientItIsEmpty(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);
        heap.insert(new ItemStack(Items.DIAMOND, 1), false);
        heap.extract(1, false);

        check(!heap.getUpdateTag(helper.getLevel().registryAccess()).isEmpty(),
                "an empty heap must still send something, or the client keeps the old picture");
        helper.succeed();
    }

    /**
     * Breaking a heap must leave one thing on the floor, not two answers to the same
     * question.
     *
     * <p>This is the duplication bug InfChest shipped and had to fix in 21.8.1: its
     * block entity's superclass dropped the inventory when the block was removed,
     * while the dropped chest was also carrying everything, so breaking one gave both
     * copies. The heap avoids it by not being a container in the first place — but
     * "avoids it by construction" is exactly the kind of claim that stops being true
     * quietly, so it is pinned here rather than argued.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void spillsNothingWhenBroken(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);
        heap.insert(new ItemStack(Items.DIAMOND, MANY), false);

        BlockPos pos = helper.absolutePos(WHERE);
        helper.getLevel().destroyBlock(pos, true);

        List<ItemEntity> loose = helper.getLevel()
                .getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4.0));
        check(loose.size() == 1, "breaking a heap should leave one item, not " + loose.size());
        check(loose.get(0).getItem().has(DataComponents.BLOCK_ENTITY_DATA),
                "and that one should be the heap, carrying what was inside it");
        helper.succeed();
    }

    /**
     * Breaking a heap with the wrong thing must cost time, never the contents.
     *
     * <p>A block of metal and glass invites {@code requiresCorrectToolForDrops}, and
     * this one had it. With the block in no mining tag that meant no tool was ever
     * correct, so a heap dropped nothing at all — two billion items gone to a
     * mis-aimed swing. Even done properly it is the wrong trade here: the contents
     * are not replaceable and a forgotten pickaxe is not a reason to destroy them.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void neverVoidsItselfForWantOfAPickaxe(GameTestHelper helper) {
        place(helper);
        BlockState state = helper.getLevel().getBlockState(helper.absolutePos(WHERE));

        check(!state.requiresCorrectToolForDrops(),
                "a heap must drop whatever it is broken with, or being wrong about the tool destroys everything inside");
        check(state.is(BlockTags.MINEABLE_WITH_PICKAXE),
                "and a pickaxe should still be the quick way to break one");
        helper.succeed();
    }

    /**
     * Three digits, one decimal, one unit — and the awkward case in the middle.
     *
     * <p>Rounding is where a rule like this goes wrong: 999,999,999,999 is not far
     * enough to be a trillion, but rounded to one decimal it reads as 1000.0 of the
     * unit below, which is four digits and the wrong unit. It has to grow into the
     * next one instead.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void writesNumbersShortAndLong(GameTestHelper helper) {
        brief(0L, "0");
        brief(999L, "999");
        brief(1_000L, "1K");
        brief(1_500L, "1.5K");
        brief(12_345L, "12.3K");
        brief(100_000_000L, "100M");
        brief(2_100_000_000L, "2.1G");
        brief(999_999_999_999L, "1T");
        brief(Long.MAX_VALUE, "9.2E");

        check(Counts.exact(2_000_000_000L).equals("2,000,000,000"),
                "the exact form keeps its commas, and got " + Counts.exact(2_000_000_000L));
        helper.succeed();
    }

    private static void brief(long count, String expected) {
        String written = Counts.brief(count);
        check(written.equals(expected), count + " should read as " + expected + ", not " + written);
    }

    /**
     * An empty heap is not committed by being right-clicked.
     *
     * <p>A pipe deciding what an empty heap is for is the point of a pipe. A player
     * who right-clicked to look inside, and found the block silently committed to
     * whatever was in their hand, has been caught by the same rule rather than served
     * by it — so the two ask different questions.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void anEmptyHeapWaitsForAPipeNotAGlance(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);
        ItemStack anything = new ItemStack(Items.DIAMOND);

        check(!heap.holds(anything), "an empty heap holds nothing, so it is not what a player is carrying");
        check(heap.accepts(anything), "but it still takes whatever a pipe offers it");

        heap.insert(anything, false);
        check(heap.holds(new ItemStack(Items.DIAMOND)), "once it holds something, that is what it holds");
        check(!heap.holds(new ItemStack(Items.GOLD_INGOT)), "and not anything else");
        helper.succeed();
    }

    /**
     * Nothing handed out is the heap's own copy of anything.
     *
     * <p>This is the shape that duplicates by the billion rather than by the stack.
     * If a returned stack points at internal state, then every {@code grow} a pipe
     * performs on it lands inside the block — and a pipe does that many times a tick,
     * so the count runs away at a rate no single call could explain. InfChest's
     * handler ends with {@code item.setCount(...); return item;} under a comment
     * saying it is "safe to modify as item is already copied", which is a comment one
     * writes after finding out.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void handsOutNothingItStillOwns(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);
        heap.insert(new ItemStack(Items.DIAMOND, MANY), false);
        IItemHandler handler = handler(helper, WHERE, null);

        ItemStack shown = handler.getStackInSlot(0);
        shown.setCount(Integer.MAX_VALUE);
        check(heap.count() == MANY, "mauling what getStackInSlot returned must not reach inside");

        ItemStack taken = handler.extractItem(0, 64, false);
        taken.grow(1_000_000);
        check(heap.count() == MANY - 64, "nor must mauling what extractItem returned");

        ItemStack offered = new ItemStack(Items.DIAMOND, 64);
        handler.insertItem(0, offered, false);
        check(offered.getCount() == 64, "and inserting must leave the caller's stack alone");
        helper.succeed();
    }

    /** Asking what would happen must not make it happen, at either end. */
    @GameTest(template = TestStructures.FLOOR)
    public static void simulatingChangesNothing(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);
        heap.insert(new ItemStack(Items.DIAMOND, MANY), false);
        IItemHandler handler = handler(helper, WHERE, null);

        for (int round = 0; round < 100; round++) {
            handler.extractItem(0, Integer.MAX_VALUE, true);
            handler.insertItem(0, new ItemStack(Items.DIAMOND, 64), true);
            handler.getStackInSlot(0);
        }

        check(heap.count() == MANY,
                "a hundred simulated rounds should have left " + MANY + ", not " + heap.count());
        helper.succeed();
    }

    /**
     * A pipe that pulls out of a heap and puts straight back into it. The loop that
     * turns a small mistake into billions a second, run against itself.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void survivesPullingIntoItself(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);
        heap.insert(new ItemStack(Items.DIAMOND, MANY), false);
        IItemHandler handler = handler(helper, WHERE, null);

        for (int round = 0; round < 500; round++) {
            int peeked = handler.extractItem(0, 64, true).getCount();
            ItemStack taken = handler.extractItem(0, 64, false);
            check(taken.getCount() == peeked, "the simulated answer must be the real one");

            ItemStack back = handler.insertItem(0, taken, false);
            check(back.isEmpty(), "and it must all go back in");
        }

        check(heap.count() == MANY,
                "five hundred round trips should have left " + MANY + ", not " + heap.count());
        helper.succeed();
    }

    /**
     * Two heaps piped into a loop, moved at the largest amount there is.
     *
     * <p>This is the setup that duplicated an InfChest by the billion: two of them
     * connected so that everything goes across and straight back, every tick, with an
     * upgrade that asks for all of it at once. A single call being slightly wrong is
     * invisible; the same call in a loop at two billion a tick is not.
     *
     * <p>The grand total is what is checked, because in a loop that is the only thing
     * that means anything — either heap can legitimately be full or empty at any
     * point.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void twoHeapsInALoopConserveEverything(GameTestHelper helper) {
        HeapBlockEntity first = place(helper, WHERE);
        HeapBlockEntity second = place(helper, OTHER);
        IItemHandler from = handler(helper, WHERE, null);
        IItemHandler into = handler(helper, OTHER, null);

        long started = 2_000_000_000L;
        first.insert(new ItemStack(Items.DIAMOND, (int) started), false);
        check(first.count() == started, "the first heap should be holding " + started);

        for (int tick = 0; tick < 200; tick++) {
            move(from, into);
            move(into, from);
            long total = first.count() + second.count();
            check(total == started,
                    "after " + tick + " round trips there should still be " + started + ", not " + total);
        }
        helper.succeed();
    }

    /** One pipe's worth of work: take everything offered, put back whatever would not fit. */
    private static void move(IItemHandler from, IItemHandler into) {
        ItemStack taken = from.extractItem(0, Integer.MAX_VALUE, false);
        if (taken.isEmpty()) {
            return;
        }
        ItemStack refused = into.insertItem(0, taken, false);
        if (!refused.isEmpty()) {
            ItemStack lost = from.insertItem(0, refused, false);
            check(lost.isEmpty(), "putting the refused items back must always work");
        }
    }

    /**
     * Past two billion the window saturates, and saturating is the whole requirement.
     *
     * <p>An item stack counts in an int, so an item handler cannot say a number larger
     * than {@code Integer.MAX_VALUE} however much is really there. That much is the
     * old API's ceiling and not something a heap can fix — it is why NeoForge's newer
     * resource handlers count in longs. What a heap must not do is <em>wrap</em>:
     * reporting a negative count would be worse than reporting a smaller one, and it
     * is the arithmetic that is easy to get wrong when the total is a long and
     * everything it is compared against is not.
     *
     * <p>Underneath, the heap is unaffected: the count is right, and draining works
     * because each extraction can take another two billion.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void saturatesRatherThanWrapsPastTwoBillion(GameTestHelper helper) {
        long was = AcervusConfig.CAPACITY.get();
        try {
            AcervusConfig.CAPACITY.set(Long.MAX_VALUE);
            HeapBlockEntity heap = place(helper);
            IItemHandler handler = handler(helper, WHERE, null);

            long each = Integer.MAX_VALUE;
            for (int time = 0; time < 3; time++) {
                heap.insert(new ItemStack(Items.DIAMOND, (int) each), false);
            }
            long held = each * 3;
            check(heap.count() == held, "the heap should be holding " + held + ", not " + heap.count());

            check(handler.getStackInSlot(0).getCount() == Integer.MAX_VALUE,
                    "the window should saturate, and read " + handler.getStackInSlot(0).getCount());
            check(handler.getSlotLimit(0) == Integer.MAX_VALUE,
                    "so should the limit, and read " + handler.getSlotLimit(0));

            long drained = 0;
            for (int sweep = 0; sweep < 10 && !heap.isEmpty(); sweep++) {
                drained += handler.extractItem(0, Integer.MAX_VALUE, false).getCount();
            }
            check(heap.isEmpty(), "and draining should still finish, leaving " + heap.count());
            check(drained == held, "having given back all " + held + ", not " + drained);
            helper.succeed();
        } finally {
            AcervusConfig.CAPACITY.set(was);
        }
    }

    /**
     * A fluid heap holds far past what a fluid stack can count, and says so as far as
     * it can.
     *
     * <p>Everything at the edge here is an int — the stack, the tank capacity, fill
     * and drain alike — so unlike the item side there is no allowance to report more.
     * What matters is that the ceiling bounds what is <em>said</em> and not what is
     * <em>held</em>: a call is not a lifetime, and repeating one fills a heap as far
     * as its capacity goes.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void aFluidHeapFillsPastWhatItCanSay(GameTestHelper helper) {
        helper.setBlock(WHERE, AcervusRegistry.FLUID_HEAP.get());
        if (!(helper.getBlockEntity(WHERE) instanceof FluidHeapBlockEntity heap)) {
            throw new GameTestAssertException("placing a fluid heap should have made one");
        }
        IFluidHandler tank = helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK, helper.absolutePos(WHERE), null);
        if (tank == null) {
            throw new GameTestAssertException("a fluid heap should offer a fluid handler to pipes");
        }

        long poured = 0;
        for (int time = 0; time < 3; time++) {
            poured += tank.fill(new FluidStack(Fluids.WATER, Integer.MAX_VALUE), IFluidHandler.FluidAction.EXECUTE);
        }

        check(heap.amount() == poured, "the heap should hold every drop poured in, and holds " + heap.amount());
        check(poured > Integer.MAX_VALUE, "and three full fills should have gone past what an int counts");
        check(tank.getFluidInTank(0).getAmount() == Integer.MAX_VALUE,
                "what it says should saturate rather than wrap, and reads "
                        + tank.getFluidInTank(0).getAmount());

        long drained = 0;
        for (int time = 0; time < 5 && !heap.isEmpty(); time++) {
            drained += tank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE).getAmount();
        }
        check(drained == poured, "and draining should give back all " + poured + ", not " + drained);
        helper.succeed();
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void aFluidHeapRefusesASecondFluid(GameTestHelper helper) {
        helper.setBlock(WHERE, AcervusRegistry.FLUID_HEAP.get());
        if (!(helper.getBlockEntity(WHERE) instanceof FluidHeapBlockEntity heap)) {
            throw new GameTestAssertException("placing a fluid heap should have made one");
        }

        heap.insert(new FluidStack(Fluids.WATER, 1_000), false);

        check(heap.insert(new FluidStack(Fluids.LAVA, 1_000), false) == 0,
                "a heap holding water should refuse lava");
        check(heap.holds(new FluidStack(Fluids.WATER, 1)), "and should know what it does hold");
        helper.succeed();
    }

    /**
     * An energy heap holds far past what {@code IEnergyStorage} can report, and every
     * number it reports saturates rather than wrapping.
     *
     * <p>This is the strictest of the three: an item handler is allowed to report more
     * than a stack and a fluid handler at least measures the same unit it moves, but
     * every single number in the energy interface is an int, including the two that
     * only describe. So a heap holding a trillion reads as two billion of two billion
     * — full — to anything that only knows how to ask, while still accepting and
     * giving out correctly.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void anEnergyHeapSaturatesRatherThanWraps(GameTestHelper helper) {
        helper.setBlock(WHERE, AcervusRegistry.ENERGY_HEAP.get());
        if (!(helper.getBlockEntity(WHERE) instanceof EnergyHeapBlockEntity heap)) {
            throw new GameTestAssertException("placing an energy heap should have made one");
        }
        IEnergyStorage cell = helper.getLevel().getCapability(
                Capabilities.EnergyStorage.BLOCK, helper.absolutePos(WHERE), null);
        if (cell == null) {
            throw new GameTestAssertException("an energy heap should offer energy storage to cables");
        }

        long received = 0;
        for (int time = 0; time < 3; time++) {
            received += cell.receiveEnergy(Integer.MAX_VALUE, false);
        }

        check(heap.stored() == received, "the heap should hold every unit taken in");
        check(received > Integer.MAX_VALUE, "three full fills should be past what an int counts");
        check(cell.getEnergyStored() == Integer.MAX_VALUE,
                "what it reports should saturate, and reads " + cell.getEnergyStored());
        check(cell.getEnergyStored() >= 0, "and must never come back negative");

        long given = 0;
        for (int time = 0; time < 5 && !heap.isEmpty(); time++) {
            given += cell.extractEnergy(Integer.MAX_VALUE, false);
        }
        check(given == received, "and it should give back all " + received + ", not " + given);
        helper.succeed();
    }

    /** Simulating must not move anything, at either end. */
    @GameTest(template = TestStructures.FLOOR)
    public static void anEnergyHeapSimulatesWithoutMoving(GameTestHelper helper) {
        helper.setBlock(WHERE, AcervusRegistry.ENERGY_HEAP.get());
        if (!(helper.getBlockEntity(WHERE) instanceof EnergyHeapBlockEntity heap)) {
            throw new GameTestAssertException("placing an energy heap should have made one");
        }
        heap.receive(1_000_000, false);

        IEnergyStorage cell = helper.getLevel().getCapability(
                Capabilities.EnergyStorage.BLOCK, helper.absolutePos(WHERE), null);
        for (int round = 0; round < 100; round++) {
            cell.receiveEnergy(1_000, true);
            cell.extractEnergy(1_000, true);
        }

        check(heap.stored() == 1_000_000,
                "a hundred simulated rounds should have left 1,000,000, not " + heap.stored());
        helper.succeed();
    }

    /**
     * An energy heap gives what it holds to what is next to it, without being asked.
     *
     * <p>Pushing is the Forge Energy convention and not a preference: a store offers,
     * a machine waits. Reasoning from "a heap is a container, so let things come and
     * take" produced a block that filled happily and never gave anything back to
     * anything — which passed every test about extraction, because every one of them
     * did the asking itself.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void anEnergyHeapOffersItselfToItsNeighbours(GameTestHelper helper) {
        helper.setBlock(WHERE, AcervusRegistry.ENERGY_HEAP.get());
        helper.setBlock(WHERE.above(), AcervusRegistry.ENERGY_HEAP.get());
        if (!(helper.getBlockEntity(WHERE) instanceof EnergyHeapBlockEntity from)
                || !(helper.getBlockEntity(WHERE.above()) instanceof EnergyHeapBlockEntity into)) {
            throw new GameTestAssertException("placing two energy heaps should have made two");
        }
        from.receive(1_000_000, false);

        EnergyHeapBlockEntity.serverTick(helper.getLevel(),
                helper.absolutePos(WHERE), helper.getBlockState(WHERE), from);

        check(into.stored() > 0, "the heap above should have been given something, and has nothing");
        check(from.stored() + into.stored() == 1_000_000,
                "and the two together should still hold 1,000,000, not "
                        + (from.stored() + into.stored()));
        helper.succeed();
    }

    /**
     * A heap in a pocket takes what is walked over, before the inventory sees it.
     *
     * <p>The check that matters is the last one: the diamonds are in the heap and
     * <em>not</em> in a slot. Absorbing that also left a copy in the inventory would
     * be a duplication bug that looks like a feature working.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void aCarriedHeapTakesWhatIsWalkedOver(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, carried(helper, new ItemStack(Items.DIAMOND, 100)));
        ItemEntity dropped = drop(helper, new ItemStack(Items.DIAMOND, 10));

        Carried.onPickup(new ItemEntityPickupEvent.Pre(player, dropped));

        check(carriedCount(player, 0) == 110,
                "a carried heap should be holding 110, not "
                        + carriedCount(player, 0));
        check(dropped.isRemoved(), "and nothing should be left lying on the ground");
        check(!player.getInventory().contains(new ItemStack(Items.DIAMOND)),
                "and no diamond should have reached a slot as well as the heap");
        helper.succeed();
    }

    /** Anything else goes past it, to be picked up the ordinary way. */
    @GameTest(template = TestStructures.FLOOR)
    public static void aCarriedHeapTakesOnlyItsOwnKind(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, carried(helper, new ItemStack(Items.DIAMOND, 100)));
        ItemEntity dropped = drop(helper, new ItemStack(Items.DIRT, 10));

        Carried.onPickup(new ItemEntityPickupEvent.Pre(player, dropped));

        check(carriedCount(player, 0) == 100,
                "a heap of diamonds should not have taken dirt");
        check(dropped.getItem().getCount() == 10, "and the dirt should still be there for the player");
        helper.succeed();
    }

    /**
     * An empty carried heap claims nothing.
     *
     * <p>The block lets a pipe decide what an empty heap is for, because that is what a
     * pipe is. Walking over something is not a decision, and a heap that committed
     * itself to the first flower picked up would be ruined by a step.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void anEmptyCarriedHeapCommitsToNothing(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(AcervusRegistry.HEAP_ITEM.get()));
        ItemEntity dropped = drop(helper, new ItemStack(Items.DIAMOND, 10));

        Carried.onPickup(new ItemEntityPickupEvent.Pre(player, dropped));

        check(carriedCount(player, 0) == 0,
                "an empty carried heap should still be empty");
        check(dropped.getItem().getCount() == 10, "and the diamonds should still be on the ground");
        helper.succeed();
    }

    /**
     * Two heaps and ten diamonds are still ten diamonds.
     *
     * <p>Every heap is asked in turn, so the count left has to travel between them.
     * Handing each the whole stack would have both report success and turn ten into
     * twenty — the one way this feature could create items out of nothing.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void severalCarriedHeapsShareOneStack(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, carried(helper, new ItemStack(Items.DIAMOND, 100)));
        player.getInventory().setItem(1, carried(helper, new ItemStack(Items.DIAMOND, 100)));

        int absorbed = Carried.absorb(player, new ItemStack(Items.DIAMOND, 10));

        long total = carriedCount(player, 0)
                + carriedCount(player, 1);
        check(absorbed == 10, "ten diamonds should have been taken, not " + absorbed);
        check(total == 210, "and the two heaps together should hold 210, not " + total);
        helper.succeed();
    }

    /**
     * Two heaps in one slot are one set of components, so adding to "the" heap would
     * add to both. Refusing is the same answer a shulker box gives by not stacking.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void aStackOfCarriedHeapsTakesNothing(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack pair = carried(helper, new ItemStack(Items.DIAMOND, 100));
        pair.setCount(2);
        player.getInventory().setItem(0, pair);

        int absorbed = Carried.absorb(player, new ItemStack(Items.DIAMOND, 10));

        check(absorbed == 0, "a stack of two heaps should have taken nothing, and took " + absorbed);
        helper.succeed();
    }

    /** An item that was only just thrown is not free to take yet, for a heap either. */
    @GameTest(template = TestStructures.FLOOR)
    public static void aCarriedHeapWaitsOutThePickupDelay(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, carried(helper, new ItemStack(Items.DIAMOND, 100)));
        ItemEntity dropped = drop(helper, new ItemStack(Items.DIAMOND, 10));
        dropped.setPickUpDelay(40);

        Carried.onPickup(new ItemEntityPickupEvent.Pre(player, dropped));

        check(carriedCount(player, 0) == 100,
                "a heap should not have taken an item that was thrown a moment ago");
        helper.succeed();
    }

    /**
     * What the heap could not hold is handed back for the inventory, and the heap
     * plus the slot come to exactly what was on the ground.
     *
     * <p>The whole-stack case leaves nothing behind and is the easy one. This is the
     * path where the item entity survives the event and vanilla carries on with the
     * remainder, and it is the one place a number could be counted twice.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void whatDoesNotFitIsHandedBack(GameTestHelper helper) {
        long capacity = AcervusConfig.CAPACITY.get();
        AcervusConfig.CAPACITY.set(105L);
        try {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.getInventory().setItem(0, carried(helper, new ItemStack(Items.DIAMOND, 100)));
            ItemEntity dropped = drop(helper, new ItemStack(Items.DIAMOND, 10));

            Carried.onPickup(new ItemEntityPickupEvent.Pre(player, dropped));

            long inHeap = carriedCount(player, 0);
            check(inHeap == 105, "the heap should have filled to 105, not " + inHeap);
            check(!dropped.isRemoved(), "and the rest should still be lying there to pick up");
            check(dropped.getItem().getCount() == 5,
                    "which is 5 diamonds, not " + dropped.getItem().getCount());
        } finally {
            AcervusConfig.CAPACITY.set(capacity);
        }
        helper.succeed();
    }

    /**
     * The statistic names what was picked up, not air.
     *
     * <p>An emptied stack answers {@code Items.AIR}, and absorbing the whole of a drop
     * empties it — so reading the item after the shrink would have quietly credited
     * every full pickup to air for as long as a matching heap was carried. Vanilla
     * takes the item at the top of {@code playerTouch} for this reason; so does this.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void theStatisticNamesWhatWasPickedUp(GameTestHelper helper) {
        Item[] awarded = new Item[1];
        Player player = new Player(helper.getLevel(), BlockPos.ZERO, 0.0F,
                new GameProfile(UUID.randomUUID(), "test-statistic-player")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }

            @Override
            public void awardStat(Stat<?> stat, int increment) {
                if (stat.getValue() instanceof Item item) {
                    awarded[0] = item;
                }
            }
        };
        player.getInventory().setItem(0, carried(helper, new ItemStack(Items.DIAMOND, 100)));

        Carried.onPickup(new ItemEntityPickupEvent.Pre(player, drop(helper, new ItemStack(Items.DIAMOND, 10))));

        check(awarded[0] == Items.DIAMOND,
                "picking up diamonds should count as diamonds, and counted as " + awarded[0]);
        helper.succeed();
    }

    /**
     * The other half of the rule, and the reason the first half is safe: a carried heap
     * offers no handler, so nothing — no pipe, no backpack mod, no other heap — can
     * draw two billion items out of an inventory slot.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void aCarriedHeapGivesNothingBack(GameTestHelper helper) {
        ItemStack heap = carried(helper, new ItemStack(Items.DIAMOND, 100));

        check(heap.getCapability(Capabilities.ItemHandler.ITEM) == null,
                "a heap in item form must offer no item handler; taking needs the block placed");
        helper.succeed();
    }

    /**
     * What arrived in a slot some other way is swept up too.
     *
     * <p>Walking over something is only one of the ways items reach a player.
     * {@code /give}, a crafting result and a shift-click out of a chest all put things
     * straight into a slot, and a heap that collected one and not the others would be
     * a rule with no shape.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void theSweepTakesWhatArrivedInASlot(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, carried(helper, new ItemStack(Items.DIAMOND, 100)));
        player.getInventory().setItem(20, new ItemStack(Items.DIAMOND, 33));

        Carried.sweep(player);

        check(carriedCount(player, 0) == 133,
                "the heap should have swept up all 33, and holds " + carriedCount(player, 0));
        check(player.getInventory().getItem(20).isEmpty(), "leaving the slot free");
        helper.succeed();
    }

    /**
     * What is in the hand is left alone.
     *
     * <p>It is the one place to keep something a heap would otherwise claim, and it
     * has to exist: without it, carrying a heap of cobblestone would mean never being
     * able to hold a cobblestone.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void theSweepLeavesWhatIsInHand(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().selected = 3;
        player.getInventory().setItem(0, carried(helper, new ItemStack(Items.DIAMOND, 100)));
        player.getInventory().setItem(3, new ItemStack(Items.DIAMOND, 33));

        Carried.sweep(player);

        check(carriedCount(player, 0) == 100, "the heap should have taken nothing from the hand");
        check(player.getInventory().getItem(3).getCount() == 33, "and the hand should still be full");
        helper.succeed();
    }

    /**
     * The screen over a held heap shows it and takes deposits, and gives nothing back.
     *
     * <p>Every way out is closed, not just the obvious one: the slot refuses to be
     * picked up from, refuses to be shift-clicked out of, and hands back nothing when
     * asked directly. One of those left open would be the whole rule undone.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void theScreenOverAHeldHeapOnlyTakes(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(player.getInventory().selected,
                carried(helper, new ItemStack(Items.DIAMOND, 100)));
        HeapMenu menu = HeapMenu.inHand(1, player.getInventory(), InteractionHand.MAIN_HAND);
        Slot heap = menu.slots.get(0);

        check(heap.getItem().getCount() == 64, "the slot should show a stack of what is inside");
        check(!heap.mayPickup(player), "and must refuse to be taken from");
        check(heap.remove(64).isEmpty(), "and hand back nothing when asked outright");
        check(menu.quickMoveStack(player, 0).isEmpty(), "and nothing when shift-clicked");
        check(carriedCount(player, player.getInventory().selected) == 100,
                "with all 100 still inside");
        helper.succeed();
    }

    /** And the heap itself cannot be moved out from under its own screen. */
    @GameTest(template = TestStructures.FLOOR)
    public static void theHeldHeapIsFrozenWhileItsScreenIsOpen(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().selected = 4;
        player.getInventory().setItem(4, carried(helper, new ItemStack(Items.DIAMOND, 100)));
        HeapMenu menu = HeapMenu.inHand(1, player.getInventory(), InteractionHand.MAIN_HAND);

        Slot held = menu.slots.get(HOTBAR_FIRST + 4);
        check(held.getItem().getItem() == AcervusRegistry.HEAP_ITEM.get(),
                "the frozen slot should be the heap's own");
        check(!held.mayPickup(player), "which must not be picked up while its screen is open");
        check(!held.mayPlace(new ItemStack(Items.DIRT)), "nor swapped for something else");
        check(menu.quickMoveStack(player, HOTBAR_FIRST + 4).isEmpty(),
                "and must not be shift-clicked into itself");
        helper.succeed();
    }

    /**
     * An empty held heap can be committed on purpose, which walking over things never
     * does. The screen is where a decision is made; a step is not a decision.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void theScreenCommitsAnEmptyHeldHeap(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        int hand = player.getInventory().selected;
        player.getInventory().setItem(hand, new ItemStack(AcervusRegistry.HEAP_ITEM.get()));
        HeapMenu menu = HeapMenu.inHand(1, player.getInventory(), InteractionHand.MAIN_HAND);

        ItemStack left = menu.slots.get(0).safeInsert(new ItemStack(Items.DIAMOND, 10), 10);

        check(left.isEmpty(), "all ten should have gone in, and " + left.getCount() + " came back");
        check(carriedCount(player, hand) == 10, "leaving the heap holding ten");
        helper.succeed();
    }

    /**
     * What a carried heap collected is there when it is put back down.
     *
     * <p>This is the round trip the rest of the carrying tests do not make: they read
     * the item's own numbers back, which would agree with themselves even if the shape
     * written were one no block could load. {@code CustomData#loadInto} is the same
     * call {@code BlockItem} makes when the block is placed.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void whatWasCollectedSurvivesBeingPutDown(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, carried(helper, new ItemStack(Items.DIAMOND, 100)));
        Carried.onPickup(new ItemEntityPickupEvent.Pre(player, drop(helper, new ItemStack(Items.DIAMOND, 10))));

        HeapBlockEntity placed = place(helper, OTHER);
        player.getInventory().getItem(0)
                .getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY)
                .loadInto(placed, helper.getLevel().registryAccess());

        check(placed.count() == 110, "the placed heap should hold 110, not " + placed.count());
        check(placed.sample().is(Items.DIAMOND), "and should hold diamonds");
        helper.succeed();
    }

    /**
     * A fluid heap in a hand reads what it is carrying, and a bucket put in its slot
     * empties into it.
     *
     * <p>Only inward, and not only when full: the block asks whether the container is
     * full because the block can also pour back out and has to be told which way a
     * half-empty bucket was meant to go. Here there is one direction and no question.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void aHeldFluidHeapReadsAndTakes(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack item = new ItemStack(AcervusRegistry.FLUID_HEAP_ITEM.get());
        HeldFluidHeap heap = HeldFluidHeap.of(helper.getLevel().registryAccess(), item);
        heap.insert(new FluidStack(Fluids.WATER, 5_000), false);

        check(heap.amount() == 5_000, "a held fluid heap should read 5,000, not " + heap.amount());
        check(!heap.gives(), "and must not give anything back while it is being carried");

        Vessel vessel = new Vessel();
        vessel.hold(new ItemStack(Items.WATER_BUCKET));
        heap.draw(vessel);

        check(heap.amount() == 6_000, "the bucket should have gone in, leaving " + heap.amount());
        check(vessel.held().is(Items.BUCKET), "and left an empty bucket in the slot");
        check(vessel.flow() == Vessel.Flow.IN, "going inward, and saying so");
        helper.succeed();
    }

    /**
     * An energy heap in a hand does the same with a battery — charged or not.
     *
     * <p>A dev-environment battery is hard to come by, so this drives the heap's own
     * side directly and checks the one thing the vessel cannot: that what goes in stays
     * in and nothing will come back out.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void aHeldEnergyHeapReadsAndTakes(GameTestHelper helper) {
        ItemStack item = new ItemStack(AcervusRegistry.ENERGY_HEAP_ITEM.get());
        HeldEnergyHeap heap = HeldEnergyHeap.of(helper.getLevel().registryAccess(), item);

        int taken = heap.receive(1_000_000);

        check(taken == 1_000_000, "a held energy heap should have taken it all, not " + taken);
        check(heap.amount() == 1_000_000, "and read " + heap.amount());
        check(!heap.gives(), "and must not give anything back while it is being carried");
        check(!heap.hasKinds(), "energy has no kinds, here as on the block");

        Vessel vessel = new Vessel();
        vessel.hold(new ItemStack(Items.STONE));
        heap.draw(vessel);
        check(heap.amount() == 1_000_000, "and something that is not a battery changes nothing");
        check(vessel.flow() == Vessel.Flow.NONE, "and moves in no direction");
        helper.succeed();
    }

    /**
     * What a held heap collected is there when it is put back down — the round trip the
     * readings alone cannot make, since they would agree with themselves even if the
     * shape written were one no block could load.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void aHeldFluidHeapSurvivesBeingPutDown(GameTestHelper helper) {
        ItemStack item = new ItemStack(AcervusRegistry.FLUID_HEAP_ITEM.get());
        HeldFluidHeap.of(helper.getLevel().registryAccess(), item)
                .insert(new FluidStack(Fluids.LAVA, 12_345), false);

        helper.setBlock(WHERE, AcervusRegistry.FLUID_HEAP.get());
        if (!(helper.getBlockEntity(WHERE) instanceof FluidHeapBlockEntity placed)) {
            throw new GameTestAssertException("placing a fluid heap should have made one");
        }
        item.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY)
                .loadInto(placed, helper.getLevel().registryAccess());

        check(placed.amount() == 12_345, "the placed heap should hold 12,345, not " + placed.amount());
        check(placed.sample().is(Fluids.LAVA), "and should hold lava");
        helper.succeed();
    }

    /**
     * The screen over a held readout takes a container and hands it back when it closes.
     *
     * <p>The vessel belongs to the screen rather than to the item — saving it onto the
     * heap would mean a bucket could be left inside one in a pocket, which is a second
     * kind of storage nobody asked for. So closing must not swallow it.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void aHeldReadoutHandsBackWhatIsInItsSlot(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(player.getInventory().selected,
                new ItemStack(AcervusRegistry.FLUID_HEAP_ITEM.get()));
        ReadoutMenu menu = ReadoutMenu.inHand(1, player.getInventory(), InteractionHand.MAIN_HAND);

        check(!menu.heap().gives(), "a held readout must say it gives nothing");
        menu.slots.get(0).set(new ItemStack(Items.WATER_BUCKET));
        menu.broadcastChanges();

        check(menu.heap().amount() == 1_000,
                "the bucket should have emptied into it, leaving " + menu.heap().amount());

        menu.removed(player);
        check(player.getInventory().contains(new ItemStack(Items.BUCKET)),
                "and the empty bucket should have come back when the screen closed");
        check(menu.slots.get(0).getItem().isEmpty(), "with nothing left in the slot");
        helper.succeed();
    }

    /**
     * Every heap an item can be written by hand must survive being saved.
     *
     * <p>This is the one that was missing, and it cost a world. {@code BLOCK_ENTITY_DATA}
     * is persisted with {@code CustomData.CODEC_WITH_ID}, which refuses a tag that does
     * not name its block entity — and it refuses it inside the player inventory save, so
     * the failure is a crash rather than a lost item. Nothing caught it earlier because
     * the network codec does not check, and because every test read the numbers back
     * through the same code that wrote them: they agreed with each other about a shape
     * the game would not accept.
     *
     * <p>So this asks the game, with the call that crashed.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void everyHeldHeapCanBeSaved(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();

        ItemStack items = new ItemStack(AcervusRegistry.HEAP_ITEM.get());
        CarriedHeap.of(registries, items).insert(new ItemStack(Items.DIAMOND, 2_308), false);
        saves(items, registries, "an item heap committed through its screen");

        ItemStack fluid = new ItemStack(AcervusRegistry.FLUID_HEAP_ITEM.get());
        HeldFluidHeap.of(registries, fluid).insert(new FluidStack(Fluids.WATER, 1_000), false);
        saves(fluid, registries, "a fluid heap filled from a bucket in its slot");

        ItemStack energy = new ItemStack(AcervusRegistry.ENERGY_HEAP_ITEM.get());
        HeldEnergyHeap.of(registries, energy).receive(1_000_000);
        saves(energy, registries, "an energy heap drained from a battery in its slot");

        helper.succeed();
    }

    /** The call the server makes on every player, once a minute, forever. */
    private static void saves(ItemStack stack, HolderLookup.Provider registries, String what) {
        try {
            stack.save(registries);
        } catch (RuntimeException refused) {
            throw new GameTestAssertException(what + " must survive a world save: " + refused.getMessage());
        }
    }

    /**
     * The same heap gives or does not give depending on where the item is.
     *
     * <p>A pocket does not give; a controller does, because a controller is a placed
     * block and placing one is the price. The reading code is identical either way —
     * what differs is one field, set where the heap was found — so this checks that the
     * field is what decides, on all three, rather than the class.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void whatGivesIsWhereTheHeapIsKept(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();

        ItemStack items = new ItemStack(AcervusRegistry.HEAP_ITEM.get());
        CarriedHeap.of(registries, items).insert(new ItemStack(Items.DIAMOND, 500), false);
        check(CarriedHeap.of(registries, items).extract(64, false).isEmpty(),
                "a carried item heap must hand back nothing");
        check(CarriedHeap.stored(registries, items).extract(64, false).getCount() == 64,
                "and the same heap in a controller must hand over 64");

        ItemStack fluid = new ItemStack(AcervusRegistry.FLUID_HEAP_ITEM.get());
        HeldFluidHeap.of(registries, fluid).insert(new FluidStack(Fluids.WATER, 5_000), false);
        check(HeldFluidHeap.of(registries, fluid).extract(1_000, false).isEmpty(),
                "a carried fluid heap must hand back nothing");
        check(HeldFluidHeap.stored(registries, fluid).extract(1_000, false).getAmount() == 1_000,
                "and the same heap in a controller must hand over a bucket");

        ItemStack energy = new ItemStack(AcervusRegistry.ENERGY_HEAP_ITEM.get());
        HeldEnergyHeap.of(registries, energy).receive(1_000);
        check(HeldEnergyHeap.of(registries, energy).give(500) == 0,
                "a carried energy heap must hand back nothing");
        check(HeldEnergyHeap.stored(registries, energy).give(500) == 500,
                "and the same heap in a controller must hand over 500");

        helper.succeed();
    }

    /**
     * Drained to nothing, a stored heap forgets its kind and loses the component
     * outright — so it stacks with the other empty ones again, and the next thing put
     * into it is not refused for a reason nothing on the item explains.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void anEmptiedStoredHeapForgetsWhatItHeld(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        ItemStack fluid = new ItemStack(AcervusRegistry.FLUID_HEAP_ITEM.get());
        HeldFluidHeap.stored(registries, fluid).insert(new FluidStack(Fluids.LAVA, 1_000), false);

        HeldFluidHeap.stored(registries, fluid).extract(1_000, false);

        check(!fluid.has(DataComponents.BLOCK_ENTITY_DATA),
                "an emptied heap should carry no contents at all");
        check(HeldFluidHeap.stored(registries, fluid).insert(new FluidStack(Fluids.WATER, 1), false) == 1,
                "and should take water next, having forgotten the lava");
        saves(fluid, registries, "an emptied stored heap");
        helper.succeed();
    }

    /**
     * A rack offers each kind of heap it holds through the window that kind speaks.
     *
     * <p>Mixed on purpose: one item heap and one fluid heap in the same rack, reached
     * through the item handler and the fluid handler respectively. The rack itself
     * stores nothing, so what this really checks is that reading a slot as a heap and
     * writing back through it survives the round trip.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void aRackOffersEveryKindItHolds(GameTestHelper helper) {
        HorreumBlockEntity rack = rack(helper);
        HolderLookup.Provider registries = helper.getLevel().registryAccess();

        ItemStack items = new ItemStack(AcervusRegistry.HEAP_ITEM.get());
        CarriedHeap.of(registries, items).insert(new ItemStack(Items.DIAMOND, MANY), false);
        rack.heaps().set(0, items);

        ItemStack fluid = new ItemStack(AcervusRegistry.FLUID_HEAP_ITEM.get());
        HeldFluidHeap.of(registries, fluid).insert(new FluidStack(Fluids.WATER, 5_000), false);
        rack.heaps().set(3, fluid);

        IItemHandler through = helper.getLevel().getCapability(
                Capabilities.ItemHandler.BLOCK, helper.absolutePos(WHERE), null);
        check(through != null && through.getSlots() == HorreumBlockEntity.SLOTS,
                "a rack should offer one handler slot per rack slot");
        check(through.getStackInSlot(0).getCount() == MANY,
                "and report all " + MANY + " of the heap in slot 0, not "
                        + through.getStackInSlot(0).getCount());
        check(through.getStackInSlot(1).isEmpty(), "and nothing for a slot with no heap in it");
        check(through.extractItem(0, 64, false).getCount() == 64,
                "and hand over 64 when asked, because a rack is a placed block");

        IFluidHandler tanks = helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK, helper.absolutePos(WHERE), null);
        // One tank per fluid heap in the rack, not one per rack slot: the heap put in
        // slot 3 is the rack's only fluid heap, so it is tank 0. Anything that lists a
        // block's tanks would otherwise draw eleven bars that say Empty forever.
        check(tanks != null && tanks.getTanks() == 1,
                "a rack with one fluid heap should offer one tank, not " + tanks.getTanks());
        check(tanks.getFluidInTank(0).getAmount() == 5_000,
                "and that tank should read 5,000");
        check(tanks.drain(1_000, IFluidHandler.FluidAction.EXECUTE).getAmount() == 1_000,
                "and hand over a bucket");
        check(CarriedHeap.of(registries, items).count() == MANY - 64
                        && HeldFluidHeap.of(registries, fluid).amount() == 4_000,
                "and both heaps should have been written back where they sit");
        helper.succeed();
    }

    /**
     * Filling looks for a heap that already holds that fluid before it commits an empty
     * one. The other way round, a rack fills up with half-used tanks of the same thing
     * while a matching heap stands beside them.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void aRackFillsWhatItAlreadyHoldsFirst(GameTestHelper helper) {
        HorreumBlockEntity rack = rack(helper);
        HolderLookup.Provider registries = helper.getLevel().registryAccess();

        ItemStack empty = new ItemStack(AcervusRegistry.FLUID_HEAP_ITEM.get());
        ItemStack water = new ItemStack(AcervusRegistry.FLUID_HEAP_ITEM.get());
        HeldFluidHeap.of(registries, water).insert(new FluidStack(Fluids.WATER, 1_000), false);
        rack.heaps().set(0, empty);
        rack.heaps().set(1, water);

        rack.fluids().fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);

        check(HeldFluidHeap.of(registries, water).amount() == 2_000,
                "the heap already holding water should have taken it");
        check(HeldFluidHeap.of(registries, empty).isEmpty(),
                "and the empty one should still be empty and uncommitted");
        helper.succeed();
    }

    /** Energy has no kinds, so a rack of energy heaps is one pool with one total. */
    @GameTest(template = TestStructures.FLOOR)
    public static void aRackOfEnergyHeapsIsOnePool(GameTestHelper helper) {
        HorreumBlockEntity rack = rack(helper);
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        for (int slot = 0; slot < 3; slot++) {
            ItemStack cell = new ItemStack(AcervusRegistry.ENERGY_HEAP_ITEM.get());
            HeldEnergyHeap.of(registries, cell).receive(1_000);
            rack.heaps().set(slot, cell);
        }

        check(rack.energy().getEnergyStored() == 3_000,
                "three heaps of 1,000 should read 3,000, not " + rack.energy().getEnergyStored());
        check(rack.energy().extractEnergy(2_500, false) == 2_500,
                "and 2,500 should come out across them");
        check(rack.energy().getEnergyStored() == 500, "leaving 500");
        helper.succeed();
    }

    /** Simulating must move nothing, at any of the three windows. */
    @GameTest(template = TestStructures.FLOOR)
    public static void aRackSimulatesWithoutMoving(GameTestHelper helper) {
        HorreumBlockEntity rack = rack(helper);
        HolderLookup.Provider registries = helper.getLevel().registryAccess();

        ItemStack items = new ItemStack(AcervusRegistry.HEAP_ITEM.get());
        CarriedHeap.of(registries, items).insert(new ItemStack(Items.DIAMOND, 1_000), false);
        rack.heaps().set(0, items);
        ItemStack cell = new ItemStack(AcervusRegistry.ENERGY_HEAP_ITEM.get());
        HeldEnergyHeap.of(registries, cell).receive(1_000);
        rack.heaps().set(1, cell);

        for (int round = 0; round < 50; round++) {
            rack.items().extractItem(0, 64, true);
            rack.items().insertItem(0, new ItemStack(Items.DIAMOND, 64), true);
            rack.energy().extractEnergy(100, true);
            rack.energy().receiveEnergy(100, true);
        }

        check(CarriedHeap.of(registries, items).count() == 1_000,
                "fifty simulated rounds should have left 1,000 diamonds, not "
                        + CarriedHeap.of(registries, items).count());
        check(rack.energy().getEnergyStored() == 1_000, "and 1,000 FE");
        helper.succeed();
    }

    /**
     * A rack carries its heaps when it is broken, and they must survive the saving.
     *
     * <p>The rack's own contents go through {@code ContainerHelper}, which is a
     * different path from the player inventory that crashed once already. Reading them
     * back through the code that wrote them is exactly the check that missed it, so
     * this asks the game to save the item instead.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void aRackCarriesItsHeapsWhenBroken(GameTestHelper helper) {
        HorreumBlockEntity rack = rack(helper);
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        ItemStack items = new ItemStack(AcervusRegistry.HEAP_ITEM.get());
        CarriedHeap.of(registries, items).insert(new ItemStack(Items.DIAMOND, MANY), false);
        rack.heaps().set(5, items);

        List<ItemStack> dropped = net.minecraft.world.level.block.Block.getDrops(
                helper.getBlockState(WHERE), helper.getLevel(), helper.absolutePos(WHERE), rack);

        check(dropped.size() == 1, "breaking a rack should leave one item, not " + dropped.size());
        saves(dropped.get(0), registries, "a rack with a heap in it");

        HorreumBlockEntity placed = rack(helper, OTHER);
        dropped.get(0).getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY)
                .loadInto(placed, registries);
        check(CarriedHeap.of(registries, placed.heap(5)).count() == MANY,
                "and putting it back down should give back all " + MANY);
        helper.succeed();
    }

    /**
     * Taking one heap out of a carried rack leaves the other eleven where they were.
     *
     * <p>It did not. The carried rack wrote its heaps as the whole component instead of
     * nesting them under {@code Heaps} the way the block entity does, so one write made
     * the other three readers - the block, the tooltip text and the tooltip picture -
     * find nothing at all. The heaps were never destroyed; nothing could read them.
     *
     * <p>So this writes with one and reads with the others, which is the only shape of
     * test that would have caught it.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void takingOneHeapOutOfACarriedRackKeepsTheRest(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack rack = new ItemStack(AcervusRegistry.HORREUM_ITEM.get());

        NonNullList<ItemStack> heaps = NonNullList.withSize(HorreumBlockEntity.SLOTS, ItemStack.EMPTY);
        for (int slot = 0; slot < 3; slot++) {
            ItemStack heap = new ItemStack(AcervusRegistry.HEAP_ITEM.get());
            CarriedHeap.of(registries, heap).insert(new ItemStack(Items.DIAMOND, 100 + slot), false);
            heaps.set(slot, heap);
        }
        HorreumBlockEntity.writeHeaps(rack, heaps, registries);
        player.getInventory().setItem(player.getInventory().selected, rack);

        HorreumMenu menu = HorreumMenu.inHand(1, player.getInventory(), InteractionHand.MAIN_HAND);
        ItemStack taken = menu.slots.get(0).remove(1);

        check(CarriedHeap.of(registries, taken).count() == 100, "the heap taken out should hold 100");
        NonNullList<ItemStack> left = HorreumBlockEntity.readHeaps(rack, registries);
        check(left.get(0).isEmpty(), "its slot should be empty now");
        check(CarriedHeap.of(registries, left.get(1)).count() == 101
                        && CarriedHeap.of(registries, left.get(2)).count() == 102,
                "and the other two should still be there, holding 101 and 102");

        // Read back the way a placed rack reads it, which is the reader that stopped
        // finding anything.
        HorreumBlockEntity placed = rack(helper);
        rack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY)
                .loadInto(placed, registries);
        check(CarriedHeap.of(registries, placed.heap(2)).count() == 102,
                "and a block loading the same item should find them too");
        saves(rack, registries, "a carried rack a heap was taken out of");
        helper.succeed();
    }

    private static HorreumBlockEntity rack(GameTestHelper helper) {
        return rack(helper, WHERE);
    }

    private static HorreumBlockEntity rack(GameTestHelper helper, BlockPos where) {
        helper.setBlock(where, AcervusRegistry.HORREUM.get());
        if (helper.getBlockEntity(where) instanceof HorreumBlockEntity rack) {
            return rack;
        }
        throw new GameTestAssertException("placing a rack should have made a rack block entity");
    }

    /** What the heap in that inventory slot is holding. */
    private static long carriedCount(Player player, int slot) {
        return CarriedHeap.of(player, player.getInventory().getItem(slot)).count();
    }

    /** A heap item holding what a real heap would hold, written the way a broken one is. */
    private static ItemStack carried(GameTestHelper helper, ItemStack contents) {
        HeapBlockEntity heap = place(helper);
        heap.insert(contents, false);
        ItemStack stack = new ItemStack(AcervusRegistry.HEAP_ITEM.get());
        heap.saveToItem(stack, helper.getLevel().registryAccess());
        helper.setBlock(WHERE, Blocks.AIR);
        return stack;
    }

    private static ItemEntity drop(GameTestHelper helper, ItemStack stack) {
        Vec3 at = Vec3.atCenterOf(helper.absolutePos(WHERE));
        ItemEntity entity = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, stack);
        entity.setNoPickUpDelay();
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    private static HeapBlockEntity place(GameTestHelper helper) {
        return place(helper, WHERE);
    }

    private static HeapBlockEntity place(GameTestHelper helper, BlockPos where) {
        helper.setBlock(where, AcervusRegistry.HEAP.get());
        if (helper.getBlockEntity(where) instanceof HeapBlockEntity heap) {
            return heap;
        }
        throw new GameTestAssertException("placing a heap should have made a heap block entity");
    }

    private static IItemHandler handler(GameTestHelper helper, BlockPos where, Direction side) {
        IItemHandler handler = helper.getLevel().getCapability(
                Capabilities.ItemHandler.BLOCK, helper.absolutePos(where), side);
        if (handler == null) {
            throw new GameTestAssertException("a heap should offer an item handler to hoppers and pipes");
        }
        return handler;
    }

    private static void check(boolean condition, String expectation) {
        if (!condition) {
            throw new GameTestAssertException(expectation);
        }
    }
}
