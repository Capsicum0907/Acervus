package io.github.capsicum0907.acervus;

import java.util.Optional;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public record Icon(Kind kind, ItemStack item, FluidStack fluid, Optional<ResourceLocation> sprite, int tint) {
    public enum Kind {
        ITEM, FLUID, SPRITE, ENERGY, MISSING
    }

    private static final int UNTINTED = 0xFFFFFFFF;

    public static final Icon ENERGY = new Icon(Kind.ENERGY, ItemStack.EMPTY, FluidStack.EMPTY, Optional.empty(), UNTINTED);
    public static final Icon MISSING = new Icon(Kind.MISSING, ItemStack.EMPTY, FluidStack.EMPTY, Optional.empty(), UNTINTED);

    public static Icon of(ItemStack item) {
        return new Icon(Kind.ITEM, item.copyWithCount(1), FluidStack.EMPTY, Optional.empty(), UNTINTED);
    }

    public static Icon of(FluidStack fluid) {
        return new Icon(Kind.FLUID, ItemStack.EMPTY, fluid.copyWithAmount(1), Optional.empty(), UNTINTED);
    }

    public static Icon sprite(ResourceLocation sprite, int tint) {
        return sprite == null ? MISSING : new Icon(Kind.SPRITE, ItemStack.EMPTY, FluidStack.EMPTY, Optional.of(sprite), tint);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, Icon> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(index -> Kind.values()[index], Kind::ordinal), Icon::kind,
            ItemStack.OPTIONAL_STREAM_CODEC, Icon::item,
            FluidStack.OPTIONAL_STREAM_CODEC, Icon::fluid,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), Icon::sprite,
            ByteBufCodecs.INT, Icon::tint,
            Icon::new);
}
