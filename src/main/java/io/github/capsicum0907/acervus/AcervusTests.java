package io.github.capsicum0907.acervus;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.github.capsicum0907.acervus.data.TestStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.stats.Stat;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
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

@GameTestHolder(Acervus.MODID)
@PrefixGameTestTemplate(false)
public final class AcervusTests {
    private static final BlockPos WHERE = new BlockPos(2, 1, 2);

    private static final BlockPos OTHER = new BlockPos(2, 1, 4);

    private static final int MANY = 5_000;

    private static final int HOTBAR_FIRST = 2 + 27;

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

    @GameTest(template = TestStructures.FLOOR)
    public static void givesNoMoreThanWasAskedFor(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);
        heap.insert(new ItemStack(Items.DIAMOND, MANY), false);

        ItemStack out = handler(helper, WHERE, null).extractItem(0, 64, false);

        check(out.getCount() == 64, "asking for 64 should give 64, not " + out.getCount());
        check(heap.count() == MANY - 64, "and take exactly that many out of the heap");
        helper.succeed();
    }

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

    @GameTest(template = TestStructures.FLOOR)
    public static void tellsTheClientItIsEmpty(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);
        heap.insert(new ItemStack(Items.DIAMOND, 1), false);
        heap.extract(1, false);

        check(!heap.getUpdateTag(helper.getLevel().registryAccess()).isEmpty(),
                "an empty heap must still send something, or the client keeps the old picture");
        helper.succeed();
    }

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

    @GameTest(template = TestStructures.FLOOR)
    public static void aCarriedHeapGivesNothingBack(GameTestHelper helper) {
        ItemStack heap = carried(helper, new ItemStack(Items.DIAMOND, 100));

        check(heap.getCapability(Capabilities.ItemHandler.ITEM) == null,
                "a heap in item form must offer no item handler; taking needs the block placed");
        helper.succeed();
    }

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

    @GameTest(template = TestStructures.FLOOR)
    public static void theScreenOverAHeldHeapOnlyTakes(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(player.getInventory().selected,
                carried(helper, new ItemStack(Items.DIAMOND, 100)));
        HeapMenu menu = HeapMenu.inHand(1, player.getInventory(), InteractionHand.MAIN_HAND);
        Slot heap = menu.slots.get(1);

        check(heap.getItem().getCount() == 64, "the slot should show a stack of what is inside");
        check(!heap.mayPickup(player), "and must refuse to be taken from");
        check(heap.remove(64).isEmpty(), "and hand back nothing when asked outright");
        check(menu.quickMoveStack(player, 1).isEmpty(), "and nothing when shift-clicked");
        check(carriedCount(player, player.getInventory().selected) == 100,
                "with all 100 still inside");
        helper.succeed();
    }

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

    @GameTest(template = TestStructures.FLOOR)
    public static void aHeldFluidHeapReadsAndTakes(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack item = new ItemStack(AcervusRegistry.FLUID_HEAP_ITEM.get());
        HeldFluidHeap heap = HeldFluidHeap.of(helper.getLevel().registryAccess(), item);
        heap.insert(new FluidStack(Fluids.WATER, 5_000), false);

        check(heap.amount() == 5_000, "a held fluid heap should read 5,000, not " + heap.amount());
        check(!heap.gives(), "and must not give anything back while it is being carried");

        Vessel vessel = new Vessel(Vessel.Flow.IN);
        vessel.hold(new ItemStack(Items.WATER_BUCKET));
        heap.draw(vessel);

        check(heap.amount() == 6_000, "the bucket should have gone in, leaving " + heap.amount());
        check(vessel.held().is(Items.BUCKET), "and left an empty bucket in the slot");
        helper.succeed();
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void aHeldEnergyHeapReadsAndTakes(GameTestHelper helper) {
        ItemStack item = new ItemStack(AcervusRegistry.ENERGY_HEAP_ITEM.get());
        HeldEnergyHeap heap = HeldEnergyHeap.of(helper.getLevel().registryAccess(), item);

        int taken = heap.receive(1_000_000);

        check(taken == 1_000_000, "a held energy heap should have taken it all, not " + taken);
        check(heap.amount() == 1_000_000, "and read " + heap.amount());
        check(!heap.gives(), "and must not give anything back while it is being carried");
        check(!heap.hasKinds(), "energy has no kinds, here as on the block");

        Vessel vessel = new Vessel(Vessel.Flow.IN);
        vessel.hold(new ItemStack(Items.STONE));
        heap.draw(vessel);
        check(heap.amount() == 1_000_000, "and something that is not a battery changes nothing");
        check(vessel.held().is(Items.STONE), "and stays where it was put");
        helper.succeed();
    }

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

    @GameTest(template = TestStructures.FLOOR)
    public static void theSlotDecidesWhichWayAContainerGoes(GameTestHelper helper) {
        FluidHeapBlockEntity heap = fluidHeap(helper);
        heap.insert(new FluidStack(Fluids.WATER, 5_000), false);
        Vessel in = heap.vessel(Vessel.Flow.IN);
        Vessel out = heap.vessel(Vessel.Flow.OUT);

        in.hold(new ItemStack(Items.BUCKET));
        out.hold(new ItemStack(Items.WATER_BUCKET));
        tick(helper, heap);
        check(heap.amount() == 5_000, "a full bucket in OUT and an empty one in IN should move nothing, and "
                + heap.amount() + " is left");
        check(in.held().is(Items.BUCKET) && out.held().is(Items.WATER_BUCKET), "and both should stay as they were");

        out.hold(new ItemStack(Items.BUCKET));
        tick(helper, heap);
        check(out.held().is(Items.WATER_BUCKET), "an empty bucket in OUT should be filled");
        check(heap.amount() == 4_000, "from the heap, leaving 4,000 rather than " + heap.amount());

        in.hold(new ItemStack(Items.WATER_BUCKET));
        tick(helper, heap);
        check(in.held().is(Items.BUCKET), "a full bucket in IN should be emptied");
        check(heap.amount() == 5_000, "into the heap, making 5,000 rather than " + heap.amount());
        helper.succeed();
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void anOldSingleSlotLandsInExactlyOneOfTheTwo(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        for (String legacy : new String[] { "IN", "OUT", "NONE" }) {
            CompoundTag tag = new CompoundTag();
            tag.put("Vessel", new ItemStack(Items.BUCKET).save(registries));
            tag.putString("VesselFlow", legacy);

            Vessel in = new Vessel(Vessel.Flow.IN);
            Vessel out = new Vessel(Vessel.Flow.OUT);
            in.load(tag, registries);
            out.load(tag, registries);

            Vessel expected = "OUT".equals(legacy) ? out : in;
            Vessel other = expected == in ? out : in;
            check(expected.held().is(Items.BUCKET), "an old " + legacy + " container should land in " + expected.flow());
            check(other.isEmpty(), "and only there, not in " + other.flow() + " as well");

            CompoundTag saved = new CompoundTag();
            in.save(saved, registries);
            out.save(saved, registries);
            check(!saved.contains("Vessel") && !saved.contains("VesselFlow"), "and the old keys must not be written back");
        }
        helper.succeed();
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void aHeldReadoutRefusesAnythingInOut(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(player.getInventory().selected,
                new ItemStack(AcervusRegistry.FLUID_HEAP_ITEM.get()));
        ReadoutMenu held = ReadoutMenu.inHand(1, player.getInventory(), InteractionHand.MAIN_HAND);
        check(!held.slots.get(1).mayPlace(new ItemStack(Items.BUCKET)), "a held heap's OUT slot must refuse a bucket");

        fluidHeap(helper);
        ReadoutMenu placed = ReadoutMenu.at(2, player.getInventory(), helper.absolutePos(WHERE));
        check(placed.slots.get(1).mayPlace(new ItemStack(Items.BUCKET)), "while a placed heap's must take one");
        helper.succeed();
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void shiftClickingSendsAnEmptyContainerOutAndAFullOneIn(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        FluidHeapBlockEntity heap = fluidHeap(helper);
        player.getInventory().setItem(9, new ItemStack(Items.BUCKET));
        player.getInventory().setItem(10, new ItemStack(Items.WATER_BUCKET));
        ReadoutMenu menu = ReadoutMenu.at(1, player.getInventory(), helper.absolutePos(WHERE));

        menu.quickMoveStack(player, 2);
        menu.quickMoveStack(player, 3);

        check(heap.vessel(Vessel.Flow.OUT).held().is(Items.BUCKET), "the empty bucket should have gone into Out");
        check(heap.vessel(Vessel.Flow.IN).held().is(Items.WATER_BUCKET), "and the full one into In");
        helper.succeed();
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void puttingAContainerInOrTakingItOutIsSaved(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        fluidHeap(helper);
        BlockPos at = helper.absolutePos(WHERE);
        net.minecraft.world.level.chunk.LevelChunk chunk = helper.getLevel().getChunkAt(at);
        ReadoutMenu menu = ReadoutMenu.at(1, player.getInventory(), at);
        Slot out = menu.slots.get(1);

        chunk.setUnsaved(false);
        out.set(new ItemStack(Items.WATER_BUCKET));
        check(chunk.isUnsaved(), "a bucket that cannot move should still be saved once it is put in");

        chunk.setUnsaved(false);
        out.remove(1);
        check(chunk.isUnsaved(), "and taking it out again should be saved too, or it comes back on reload");
        helper.succeed();
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void theBarReadsTheSameAmountThreeWays(GameTestHelper helper) {
        long capacity = 1_000_000_000_000L;
        near(BarScale.LINEAR.fraction(600_000_000_000L, capacity), 0.6, "linear, 600G of 1T");
        near(BarScale.LOG.fraction(1_000L, capacity), 0.25, "logarithmic, 1K of 1T");
        near(BarScale.DECADE.fraction(55_000L, capacity), 0.5, "within the decade, 55K between 10K and 100K");
        near(BarScale.DECADE.fraction(1_500L, 2_000L), 0.5, "within the decade, 1.5K in a window capped at a capacity of 2K");
        for (BarScale scale : BarScale.values()) {
            near(scale.fraction(0L, capacity), 0.0, scale + " when empty");
            near(scale.fraction(capacity, capacity), 1.0, scale + " when full");
        }
        check(BarScale.LOG.ticks(capacity).length == 11, "a trillion should have a tick between each of its 12 decades");
        check(BarScale.LINEAR.ticks(capacity).length == 0, "and only the logarithmic bar has ticks");
        check(BarScale.DECADE.next() == BarScale.LINEAR, "and clicking past the last goes back to the first");
        helper.succeed();
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void aPowerOfTenIsWrittenAsOne(GameTestHelper helper) {
        check(Counts.power(1L).equals("10⁰"), "1 should read 10 to the 0, not " + Counts.power(1L));
        check(Counts.power(1_000_000_000_000L).equals("10¹²"), "a trillion should read 10 to the 12");
        check(Counts.power(2_000L).equals("2,000"), "and anything else should stay a plain number");
        check(Counts.powerBuckets(10_000_000_000L).equals("10⁷ B"), "ten billion mB should read 10 to the 7 buckets");
        check(Counts.powerBuckets(100L).equals("10² mB"), "and less than a bucket should stay in mB");
        helper.succeed();
    }

    private static void near(double actual, double expected, String what) {
        check(Math.abs(actual - expected) < 1e-9, what + " should read " + expected + ", not " + actual);
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void anItemHeapTakesInOnTheLeftAndGivesOnTheRight(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        HeapBlockEntity heap = place(helper);
        HeapMenu menu = HeapMenu.at(1, player.getInventory(), helper.absolutePos(WHERE));
        Slot in = menu.slots.get(0);
        Slot out = menu.slots.get(1);

        ItemStack left = in.safeInsert(new ItemStack(Items.DIAMOND, 10), 10);
        check(left.isEmpty() && heap.count() == 10, "In should take all ten, and the heap holds " + heap.count());
        check(in.getItem().isEmpty(), "and In should show nothing afterwards");
        check(out.getItem().is(Items.DIAMOND) && out.getItem().getCount() == 10, "while Out shows what is inside");
        check(!out.mayPlace(new ItemStack(Items.DIAMOND)), "Out must not take anything in");
        check(!in.mayPlace(new ItemStack(Items.DIRT)), "and In must refuse a second kind");

        ItemStack taken = out.remove(4);
        check(taken.getCount() == 4 && heap.count() == 6, "taking four from Out should leave six, not " + heap.count());

        player.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 5));
        menu.quickMoveStack(player, 2);
        check(heap.count() == 11, "and shift-clicking five from the inventory should make eleven, not " + heap.count());
        helper.succeed();
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void anEnergyHeapLightsALampForEveryDecade(GameTestHelper helper) {
        long capacity = 1_000_000_000_000L;
        check(EnergyHeapBlock.lamps(0L, capacity) == 0, "an empty heap should light nothing");
        check(EnergyHeapBlock.lamps(9L, capacity) == 0, "9 FE should not yet light the first lamp");
        check(EnergyHeapBlock.lamps(10L, capacity) == 1, "10 FE should light the first");
        check(EnergyHeapBlock.lamps(1_000_000L, capacity) == 6, "a million should light six");
        check(EnergyHeapBlock.lamps(capacity, capacity) == EnergyHeapBlock.MAX_LAMPS, "and a full heap all of them");

        helper.setBlock(WHERE, AcervusRegistry.ENERGY_HEAP.get());
        if (!(helper.getBlockEntity(WHERE) instanceof EnergyHeapBlockEntity heap)) {
            throw new GameTestAssertException("placing an energy heap should have made one");
        }
        heap.receive(1_000_000, false);
        EnergyHeapBlockEntity.serverTick(helper.getLevel(), heap.getBlockPos(), heap.getBlockState(), heap);
        int shown = helper.getBlockState(WHERE).getValue(EnergyHeapBlock.LAMPS);
        int expected = EnergyHeapBlock.lamps(1_000_000L, heap.capacity());
        check(shown == expected, "the block itself should show " + expected + " lamps for a million, not " + shown);
        helper.succeed();
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void aRackFromBeforeNineKeepsTheRestWaiting(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (int slot = 0; slot < 12; slot++) {
            CompoundTag one = (CompoundTag) new ItemStack(AcervusRegistry.HEAP_ITEM.get()).save(registries, new CompoundTag());
            one.putByte("Slot", (byte) slot);
            list.add(one);
        }
        CompoundTag inside = new CompoundTag();
        inside.put("Items", list);
        CompoundTag tag = new CompoundTag();
        tag.put("Heaps", inside);

        HorreumBlockEntity rack = rack(helper);
        rack.loadWithComponents(tag, registries);
        check(rack.heaps().stream().noneMatch(ItemStack::isEmpty), "all nine slots should be filled");
        check(rack.waiting() == 3, "with the other three waiting, not " + rack.waiting());

        rack.heaps().set(4, ItemStack.EMPTY);
        rack.changed();
        check(!rack.heap(4).isEmpty() && rack.waiting() == 2, "an emptied slot should take the next one waiting");

        ItemStack dropped = new ItemStack(AcervusRegistry.HORREUM_ITEM.get());
        rack.saveToItem(dropped, registries);
        long carried = HorreumBlockEntity.readHeaps(dropped, registries).stream().filter(s -> !s.isEmpty()).count();
        check(carried == HorreumBlockEntity.SLOTS, "the item should show nine, and shows " + carried);
        HorreumBlockEntity.writeHeaps(dropped, HorreumBlockEntity.readHeaps(dropped, registries), registries);
        rack.loadWithComponents(dropped.get(DataComponents.BLOCK_ENTITY_DATA).copyTag(), registries);
        check(rack.waiting() == 2, "and rewriting the item must keep the two waiting, not " + rack.waiting());
        helper.succeed();
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void aLockedHeapKeepsItsKindWhenEmptied(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);
        heap.lock(true);
        check(!heap.locked(), "an empty heap has nothing to lock to");

        heap.insert(new ItemStack(Items.DIAMOND, 10), false);
        heap.lock(true);
        heap.extract(10, false);
        check(heap.isEmpty() && heap.locked(), "emptying a locked heap should leave it empty and locked");
        check(!heap.accepts(new ItemStack(Items.DIRT)), "and it must refuse another kind");
        check(heap.accepts(new ItemStack(Items.DIAMOND)), "while still taking its own");

        heap.lock(false);
        check(heap.accepts(new ItemStack(Items.DIRT)), "unlocking an empty heap should forget its kind");
        helper.succeed();
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void aLockedFluidHeapKeepsItsKindOnTheItemAndInTheMenu(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        FluidHeapBlockEntity heap = fluidHeap(helper);
        heap.insert(new FluidStack(Fluids.WATER, 1_000), false);
        ReadoutMenu menu = ReadoutMenu.at(1, player.getInventory(), helper.absolutePos(WHERE));
        menu.clickMenuButton(player, LockButton.ID);
        check(heap.locked(), "the screen's button should lock the heap");
        heap.extract(1_000, false);

        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        ItemStack item = new ItemStack(AcervusRegistry.FLUID_HEAP_ITEM.get());
        heap.saveToItem(item, registries);
        HeldFluidHeap carried = HeldFluidHeap.stored(registries, item);
        check(carried.locked() && carried.isEmpty(), "an emptied locked heap should stay locked as an item");
        check(carried.insert(new FluidStack(Fluids.LAVA, 1_000), true) == 0, "and still refuse lava");
        check(carried.insert(new FluidStack(Fluids.WATER, 1_000), true) == 1_000, "while taking water");

        carried.lock(false);
        check(carried.insert(new FluidStack(Fluids.LAVA, 1_000), true) == 1_000, "unlocking should let lava in");
        helper.succeed();
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void aRackIsMadeOnlyFromAnEmptyHeap(GameTestHelper helper) {
        ItemStack empty = new ItemStack(AcervusRegistry.HEAP_ITEM.get());
        ItemStack full = carried(helper, new ItemStack(Items.DIAMOND, 100));
        check(crafted(helper, empty).is(AcervusRegistry.HORREUM_ITEM.get()), "an empty heap should make a rack");
        check(crafted(helper, full).isEmpty(), "and a heap with something in it must not, or what it holds is lost");
        helper.succeed();
    }

    private static ItemStack crafted(GameTestHelper helper, ItemStack heap) {
        ItemStack netherite = new ItemStack(Items.NETHERITE_INGOT);
        ItemStack star = new ItemStack(Items.NETHER_STAR);
        net.minecraft.world.item.crafting.CraftingInput grid = net.minecraft.world.item.crafting.CraftingInput.of(3, 3,
                List.of(netherite, star, netherite, star, heap, star, netherite, star, netherite));
        return helper.getLevel().getRecipeManager()
                .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, grid, helper.getLevel())
                .map(found -> found.value().assemble(grid, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    private static FluidHeapBlockEntity fluidHeap(GameTestHelper helper) {
        helper.setBlock(WHERE, AcervusRegistry.FLUID_HEAP.get());
        if (helper.getBlockEntity(WHERE) instanceof FluidHeapBlockEntity heap) {
            return heap;
        }
        throw new GameTestAssertException("placing a fluid heap should have made one");
    }

    private static void tick(GameTestHelper helper, FluidHeapBlockEntity heap) {
        FluidHeapBlockEntity.serverTick(helper.getLevel(), heap.getBlockPos(), heap.getBlockState(), heap);
    }

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

    private static void saves(ItemStack stack, HolderLookup.Provider registries, String what) {
        try {
            stack.save(registries);
        } catch (RuntimeException refused) {
            throw new GameTestAssertException(what + " must survive a world save: " + refused.getMessage());
        }
    }

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

        HorreumBlockEntity placed = rack(helper);
        rack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY)
                .loadInto(placed, registries);
        check(CarriedHeap.of(registries, placed.heap(2)).count() == 102,
                "and a block loading the same item should find them too");
        saves(rack, registries, "a carried rack a heap was taken out of");
        helper.succeed();
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void everyItemHasARecipe(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();

        List<Item> craftable = helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .map(held -> held.value().getResultItem(registries).getItem())
                .toList();

        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (!Acervus.MODID.equals(id.getNamespace())) {
                continue;
            }
            check(craftable.contains(item), id + " has no recipe");
        }
        helper.succeed();
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void emptyOnesStack(GameTestHelper helper) {
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (!Acervus.MODID.equals(id.getNamespace())) {
                continue;
            }
            check(new ItemStack(item).getMaxStackSize() > 1, id + " will not stack while empty");
        }
        helper.succeed();
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void fullOnesDoNot(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();

        ItemStack heap = new ItemStack(AcervusRegistry.HEAP_ITEM.get());
        CarriedHeap.of(registries, heap).insert(new ItemStack(Items.DIAMOND, 64), false);
        check(heap.getMaxStackSize() == 1, "a heap holding diamonds should not stack");

        ItemStack rack = new ItemStack(AcervusRegistry.HORREUM_ITEM.get());
        NonNullList<ItemStack> heaps = NonNullList.withSize(HorreumBlockEntity.SLOTS, ItemStack.EMPTY);
        heaps.set(0, heap);
        HorreumBlockEntity.writeHeaps(rack, heaps, registries);
        check(rack.getMaxStackSize() == 1, "a rack holding a heap should not stack");

        CarriedHeap.stored(registries, heap).extract(64, false);
        check(heap.getMaxStackSize() > 1, "an emptied heap should stack again");
        helper.succeed();
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void aStackOfHeapsHasNoScreen(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(AcervusRegistry.HEAP_ITEM.get(), 4));

        InteractionResultHolder<ItemStack> result = Carried.open(player, InteractionHand.MAIN_HAND,
                (id, inventory, who) -> null);
        check(result.getResult() == InteractionResult.FAIL, "a stack of four should not open");

        player.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(AcervusRegistry.HEAP_ITEM.get()));
        check(Carried.open(player, InteractionHand.MAIN_HAND, (id, inventory, who) -> null)
                .getResult() != InteractionResult.FAIL, "and a single one still should");
        helper.succeed();
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void aStackHoldingSomethingIsNotPlaced(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        ItemStack heap = new ItemStack(AcervusRegistry.HEAP_ITEM.get());
        CarriedHeap.of(registries, heap).insert(new ItemStack(Items.DIAMOND, 64), false);
        heap.setCount(3);
        player.setItemInHand(InteractionHand.MAIN_HAND, heap);

        helper.setBlock(WHERE, Blocks.AIR);
        BlockPos at = helper.absolutePos(WHERE);
        BlockHitResult where = new BlockHitResult(Vec3.atCenterOf(at.below()), Direction.UP,
                at.below(), false);
        InteractionResult placed = ((ContentsBlockItem) heap.getItem()).place(
                new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, where)));

        check(placed == InteractionResult.FAIL, "a stack of three full heaps should not place");
        check(helper.getLevel().getBlockState(at).isAir(), "and nothing should be there");
        check(heap.getCount() == 3, "and none of them should have been used up");
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

    private static long carriedCount(Player player, int slot) {
        return CarriedHeap.of(player, player.getInventory().getItem(slot)).count();
    }

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
