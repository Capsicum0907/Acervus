package io.github.capsicum0907.acervus;

import java.util.function.Supplier;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * The heap that is being carried, read as a {@link Pile}.
 *
 * <p>A heap keeps its contents when it is broken, so a heap in an inventory is
 * already a full heap — the numbers are sitting in {@code BLOCK_ENTITY_DATA} with
 * nothing looking at them. This is the looking, in the same terms the block answers
 * in, so the slot and the screen do not have to know which one they are over.
 *
 * <p><b>It takes and does not give.</b> {@link #gives()} is false and every path out
 * is closed behind it. That asymmetry is the design and not an unfinished half of
 * it: collecting while mining is what makes a heap worth a slot, and drawing two
 * billion of anything out of a pocket would end every reason to carry anything else.
 * Taking still means placing the block somewhere, which is a deliberate act in a
 * place. Everything below {@code gives()} is written out in full, so the day that
 * judgement changes it is one method.
 *
 * <p>The stack is fetched afresh every time rather than held. The item can be moved,
 * swapped or dropped while a screen is open on it, and holding the object would mean
 * writing into a stack that is no longer anywhere.
 */
public final class CarriedHeap implements Pile {
    private static final String SAMPLE = "Sample";
    private static final String AMOUNT = "Amount";

    /**
     * What the count used to be called here. Read when the current name is absent, so
     * heaps saved before the four blocks agreed on one word keep their contents.
     */
    private static final String LEGACY_COUNT = "Count";

    private final HolderLookup.Provider registries;
    private final Supplier<ItemStack> where;

    /** The last parsed sample, and the data it came from, so a render loop parses once. */
    private CustomData read;
    private ItemStack parsed = ItemStack.EMPTY;

    private final boolean gives;

    private CarriedHeap(HolderLookup.Provider registries, Supplier<ItemStack> where, boolean gives) {
        this.registries = registries;
        this.where = where;
        this.gives = gives;
    }

    /** A particular stack, which the caller is holding still. */
    public static CarriedHeap of(HolderLookup.Provider registries, ItemStack stack) {
        return new CarriedHeap(registries, () -> stack, false);
    }

    /** The same, where a player is the nearest thing that knows the registries. */
    public static CarriedHeap of(Player player, ItemStack stack) {
        return of(player.level().registryAccess(), stack);
    }

    /** Whatever is in that hand at the moment of asking. */
    public static CarriedHeap inHand(Player player, InteractionHand hand) {
        return new CarriedHeap(player.level().registryAccess(), () -> player.getItemInHand(hand), false);
    }

    /** A heap slotted into a controller, which is a placed block, so it gives. */
    public static CarriedHeap stored(HolderLookup.Provider registries, ItemStack stack) {
        return new CarriedHeap(registries, () -> stack, true);
    }

    /**
     * Whether anything may come out, decided by <em>where the item is</em> rather than
     * by what it is. See {@link Held#gives()} for the reasoning; it is the same rule and
     * the same field.
     */
    @Override
    public boolean gives() {
        return gives;
    }

    @Override
    public boolean isEmpty() {
        return sample().isEmpty() || count() <= 0;
    }

    @Override
    public ItemStack sample() {
        CustomData data = where.get().getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        if (data.equals(read)) {
            return parsed;
        }
        read = data;
        CompoundTag tag = data.copyTag();
        parsed = tag.contains(SAMPLE)
                ? ItemStack.parse(registries, tag.getCompound(SAMPLE)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        return parsed;
    }

    @Override
    public long count() {
        CompoundTag tag = tag();
        return tag.contains(AMOUNT) ? tag.getLong(AMOUNT) : tag.getLong(LEGACY_COUNT);
    }

    public long capacity() {
        return AcervusConfig.CAPACITY.get();
    }

    @Override
    public long room() {
        return Math.max(0L, capacity() - count());
    }

    @Override
    public ItemStack stack() {
        ItemStack sample = sample();
        if (sample.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return sample.copyWithCount((int) Math.min(count(), sample.getMaxStackSize()));
    }

    @Override
    public boolean accepts(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        // The same answer the block gives, deliberately, including about other heaps:
        // one kind of heap behaving differently from the other is a rule nobody can see.
        return isEmpty() || ItemStack.isSameItemSameComponents(sample(), stack);
    }

    /** Whether this is already what it holds — the question a person is asked. */
    public boolean holds(ItemStack stack) {
        return !isEmpty() && !stack.isEmpty() && ItemStack.isSameItemSameComponents(sample(), stack);
    }

    @Override
    public int insert(ItemStack stack, boolean simulate) {
        if (!accepts(stack)) {
            return 0;
        }
        int taken = (int) Math.min(room(), stack.getCount());
        if (taken <= 0 || simulate) {
            return Math.max(taken, 0);
        }

        ItemStack heap = where.get();
        CompoundTag tag = tag();
        long held = isEmpty() ? 0L : count();
        if (isEmpty()) {
            tag.put(SAMPLE, stack.copyWithCount(1).save(registries));
        }
        tag.putLong(AMOUNT, held + taken);
        tag.remove(LEGACY_COUNT);
        write(heap, tag);
        return taken;
    }

    @Override
    public ItemStack extract(int amount, boolean simulate) {
        if (!gives() || isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        int taken = (int) Math.min(Math.min(amount, count()), Integer.MAX_VALUE);
        ItemStack out = sample().copyWithCount(taken);
        if (simulate) {
            return out;
        }

        ItemStack heap = where.get();
        CompoundTag tag = tag();
        long left = count() - taken;
        if (left <= 0) {
            // Emptied heaps forget their kind, here as on the block: one that remembered
            // would refuse the next thing put in with nothing to say why.
            tag.remove(SAMPLE);
            left = 0;
        }
        tag.putLong(AMOUNT, left);
        write(heap, tag);
        return out;
    }

    /** Nothing to do: the item lives in an inventory, which sends its own changes. */
    @Override
    public void setChanged() {
    }

    private CompoundTag tag() {
        return where.get().getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
    }

    /**
     * An emptied heap loses the component altogether, so that it stacks with the other
     * empty ones again instead of looking different for carrying an empty tag.
     *
     * <p><b>Through {@code setBlockEntityData}, never {@code CustomData.of}.</b> The
     * component is persisted with {@code CustomData.CODEC_WITH_ID}, which refuses a tag
     * with no {@code id} naming the block entity — and refuses it while the player's
     * inventory is being saved, which takes the world down. Nothing catches it earlier:
     * the network codec does not check, so a heap written by hand looks perfectly well
     * until the first autosave. See {@link Held#write}.
     */
    private void write(ItemStack heap, CompoundTag tag) {
        BlockItem.setBlockEntityData(heap, AcervusRegistry.HEAP_ENTITY.get(),
                tag.contains(SAMPLE) ? tag : new CompoundTag());
    }
}
