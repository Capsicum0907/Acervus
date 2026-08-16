package io.github.capsicum0907.acervus.client;

import java.util.ArrayList;
import java.util.List;

import io.github.capsicum0907.acervus.AcervusRegistry;
import io.github.capsicum0907.acervus.CarriedHeap;
import io.github.capsicum0907.acervus.Counts;
import io.github.capsicum0907.acervus.HeapContents;
import io.github.capsicum0907.acervus.HeldEnergyHeap;
import io.github.capsicum0907.acervus.HeldFluidHeap;
import io.github.capsicum0907.acervus.HorreumBlockEntity;
import io.github.capsicum0907.acervus.Mods;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import org.joml.Matrix4f;

/**
 * Draws {@link HeapContents}: one row per heap, an icon then a name then an amount.
 *
 * <p>The icon is whatever stands for the contents where they are usually seen — the
 * item itself, or a fluid's or a chemical's own sprite off the block atlas. Energy has
 * no kinds, so its row has no icon and says so by being a number on its own.
 *
 * <p>The reading is done here rather than carried, because the registries a heap needs
 * to be read are reachable from the client and not from the item. See
 * {@link HeapContents}.
 */
public class HeapContentsTooltip implements ClientTooltipComponent {
    private static final int ICON = 16;
    private static final int GAP = 4;
    private static final int ROW = 18;

    /** The amount, set apart from the name so a column of them reads as a column. */
    private static final int AMOUNT_COLOUR = 0xFFAAAAAA;
    private static final int NAME_COLOUR = 0xFFFFFFFF;

    /**
     * One heap: something to draw, what it is called, and how much of it.
     *
     * <p>Public only so the gas package can build one — it is the one kind of contents
     * this class must not read for itself.
     */
    public record Row(ItemStack item, FluidStack fluid, ResourceLocation sprite, int tint,
            Component name, String amount) {
    }

    private final List<Row> rows;

    public HeapContentsTooltip(HeapContents contents) {
        this.rows = read(contents.of());
    }

    /** Whether there is anything worth drawing, so an empty one is never offered. */
    public static boolean anything(ItemStack stack) {
        return !new HeapContentsTooltip(new HeapContents(stack)).rows.isEmpty();
    }

    private static List<Row> read(ItemStack stack) {
        List<Row> rows = new ArrayList<>();
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return rows;
        }
        HolderLookup.Provider registries = client.level.registryAccess();

        if (stack.getItem() == AcervusRegistry.HORREUM_ITEM.get()) {
            NonNullList<ItemStack> heaps =
                    NonNullList.withSize(HorreumBlockEntity.SLOTS, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(
                    stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY)
                            .copyTag().getCompound("Heaps"),
                    heaps, registries);
            for (ItemStack heap : heaps) {
                Row row = row(heap, registries);
                if (row != null) {
                    rows.add(row);
                }
            }
            return rows;
        }

        Row row = row(stack, registries);
        if (row != null) {
            rows.add(row);
        }
        return rows;
    }

    /**
     * One heap read as a row, or null when there is nothing in it.
     *
     * <p>The gas heap is asked for last and behind {@link Mods#mekanism()}, so that a
     * game without Mekanism never reaches a class that mentions a chemical.
     */
    private static Row row(ItemStack heap, HolderLookup.Provider registries) {
        Item kind = heap.getItem();

        if (kind == AcervusRegistry.HEAP_ITEM.get()) {
            CarriedHeap held = CarriedHeap.of(registries, heap);
            return held.isEmpty() ? null : new Row(held.sample(), FluidStack.EMPTY, null, 0,
                    held.sample().getHoverName(), Counts.exact(held.count()));
        }
        if (kind == AcervusRegistry.FLUID_HEAP_ITEM.get()) {
            HeldFluidHeap held = HeldFluidHeap.of(registries, heap);
            return held.isEmpty() ? null : new Row(ItemStack.EMPTY, held.sample(), null, 0,
                    held.contentName(), held.brief(held.amount()));
        }
        if (kind == AcervusRegistry.ENERGY_HEAP_ITEM.get()) {
            HeldEnergyHeap held = HeldEnergyHeap.of(registries, heap);
            return held.isEmpty() ? null : new Row(ItemStack.EMPTY, FluidStack.EMPTY, null, 0,
                    Component.translatable("gui.acervus.energy"), held.brief(held.amount()));
        }
        if (Mods.mekanism() && kind == io.github.capsicum0907.acervus.gas.GasHeap.ITEM.get()) {
            return io.github.capsicum0907.acervus.gas.GasRow.of(registries, heap);
        }
        return null;
    }

    @Override
    public int getHeight() {
        return rows.size() * ROW;
    }

    @Override
    public int getWidth(Font font) {
        int widest = 0;
        for (Row row : rows) {
            widest = Math.max(widest,
                    ICON + GAP + font.width(row.name()) + GAP + font.width(row.amount()));
        }
        return widest;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        int top = y;
        for (Row row : rows) {
            drawIcon(graphics, row, x, top);
            top += ROW;
        }
    }

    /**
     * The text is drawn with the icons rather than as tooltip lines of its own, so that
     * a name always sits beside the picture it belongs to. Tooltip text and tooltip
     * images are laid out separately, and a list of twelve would drift apart.
     */
    @Override
    public void renderText(Font font, int x, int y, Matrix4f matrix,
            MultiBufferSource.BufferSource buffer) {
        int top = y;
        for (Row row : rows) {
            float baseline = top + (ICON - font.lineHeight) / 2.0F + 1;
            int text = x + ICON + GAP;
            font.drawInBatch(row.name(), text, baseline, NAME_COLOUR, true,
                    matrix, buffer, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            font.drawInBatch(Component.literal(row.amount()),
                    text + font.width(row.name()) + GAP, baseline, AMOUNT_COLOUR, true,
                    matrix, buffer, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            top += ROW;
        }
    }

    private void drawIcon(GuiGraphics graphics, Row row, int x, int y) {
        if (!row.item().isEmpty()) {
            graphics.renderItem(row.item(), x, y);
            return;
        }

        ResourceLocation texture = row.sprite();
        int tint = row.tint();
        FluidStack fluid = row.fluid();
        if (!fluid.isEmpty()) {
            // A fluid's sprite is only reachable from the client, which is here.
            IClientFluidTypeExtensions look = IClientFluidTypeExtensions.of(fluid.getFluid());
            texture = look.getStillTexture(fluid);
            tint = 0xFF000000 | look.getTintColor(fluid);
        }
        if (texture == null) {
            return;
        }

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        graphics.setColor(
                (tint >> 16 & 0xFF) / 255.0F,
                (tint >> 8 & 0xFF) / 255.0F,
                (tint & 0xFF) / 255.0F,
                1.0F);
        graphics.blit(x, y, 0, ICON, ICON, sprite);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
