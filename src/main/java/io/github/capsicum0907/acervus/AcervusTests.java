package io.github.capsicum0907.acervus;

import java.util.List;

import io.github.capsicum0907.acervus.data.TestStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
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
