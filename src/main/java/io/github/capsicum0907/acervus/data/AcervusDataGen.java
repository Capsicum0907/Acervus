package io.github.capsicum0907.acervus.data;

import java.util.concurrent.CompletableFuture;

import io.github.capsicum0907.acervus.Acervus;
import io.github.capsicum0907.acervus.AcervusRegistry;
import io.github.capsicum0907.acervus.Mods;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Everything under {@code src/generated/resources} comes from here, so nothing in
 * that directory is written by hand.
 *
 * <p>No loot table: {@link io.github.capsicum0907.acervus.HeapBlock#getDrops} answers
 * that question in code, because what a heap drops depends on what is inside it.
 */
@EventBusSubscriber(modid = Acervus.MODID, value = { Dist.CLIENT, Dist.DEDICATED_SERVER })
public final class AcervusDataGen {
    private AcervusDataGen() {
    }

    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();

        generator.addProvider(event.includeClient(),
                new Models(output, event.getExistingFileHelper()));
        generator.addProvider(event.includeClient(), new Language(output));
        generator.addProvider(event.includeServer(), new Recipes(output, event.getLookupProvider()));
        generator.addProvider(event.includeServer(), new TestStructures(output));
        generator.addProvider(event.includeServer(),
                new Tags(output, event.getLookupProvider(), event.getExistingFileHelper()));
    }

    /**
     * A pickaxe is what breaks a heap quickly. Only quickly — the block deliberately
     * does not require a correct tool, because failing to drop would mean losing
     * everything inside it.
     */
    private static class Tags extends BlockTagsProvider {
        Tags(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                ExistingFileHelper existingFileHelper) {
            super(output, registries, Acervus.MODID, existingFileHelper);
        }

        /**
         * The gas heap goes in as an <b>optional</b> entry, always, whether Mekanism is
         * here at datagen time or not.
         *
         * <p>A required entry naming a block that does not exist does not merely go
         * missing: the whole tag file is refused, and all four blocks fall out of
         * {@code mineable/pickaxe} together. So a game without Mekanism had a heap that
         * a pickaxe was no quicker at than a fist — which is what
         * {@code neverVoidsItselfForWantOfAPickaxe} caught the moment the mods were
         * taken out of the folder.
         *
         * <p>Writing it optionally and unconditionally also ends the older trap that the
         * generated files depended on what happened to be in {@code run/mods}.
         */
        @Override
        protected void addTags(HolderLookup.Provider registries) {
            tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .add(AcervusRegistry.HEAP.get())
                    .add(AcervusRegistry.FLUID_HEAP.get())
                    .add(AcervusRegistry.ENERGY_HEAP.get())
                    .add(AcervusRegistry.HORREUM.get())
                    .addOptional(ResourceLocation.fromNamespaceAndPath(Acervus.MODID, "chemical_heap"));
        }
    }

    private static class Models extends BlockStateProvider {
        Models(PackOutput output, ExistingFileHelper existingFileHelper) {
            super(output, Acervus.MODID, existingFileHelper);
        }

        @Override
        protected void registerStatesAndModels() {
            glassBox(AcervusRegistry.HEAP);
            glassBox(AcervusRegistry.FLUID_HEAP);
            glassBox(AcervusRegistry.ENERGY_HEAP);
            // Solid: a rack holds heaps rather than contents, so there is nothing
            // to see through and no reason to pay for translucency.
            String rack = AcervusRegistry.HORREUM.getId().getPath();
            simpleBlock(AcervusRegistry.HORREUM.get(), models().cubeAll(rack, modLoc("block/" + rack)));
            itemModels().withExistingParent(rack, modLoc("block/" + rack));
            // Only when Mekanism is present, because the block only exists then. Keep
            // Mekanism in run/mods when regenerating, or these assets go stale.
            if (Mods.mekanism()) {
                glassBox(io.github.capsicum0907.acervus.gas.GasHeap.BLOCK);
            }
        }

        /**
         * translucent, because the middle of the texture is see-through and the
         * default render type would draw those pixels as fully opaque.
         */
        private void glassBox(DeferredBlock<?> block) {
            String name = block.getId().getPath();
            ModelFile model = models()
                    .cubeAll(name, modLoc("block/" + name))
                    .renderType("minecraft:translucent");

            simpleBlock(block.get(), model);
            itemModels().withExistingParent(name, modLoc("block/" + name));
        }
    }

    private static class Language extends LanguageProvider {
        Language(PackOutput output) {
            super(output, Acervus.MODID, "en_us");
        }

        @Override
        protected void addTranslations() {
            // "Item Heap", not "Heap": it is one of four, and being the first written is
            // not a reason for it to be the one without a surname.
            add(AcervusRegistry.HEAP.get(), "Item Heap");
            add(AcervusRegistry.FLUID_HEAP.get(), "Fluid Heap");
            add(AcervusRegistry.ENERGY_HEAP.get(), "Energy Heap");
            // Latin for a granary: the building heaps are kept in.
            add(AcervusRegistry.HORREUM.get(), "Horreum");
            // Shared by all four: emptiness is not a fact about any one of them.
            add("gui.acervus.empty", "Empty");
            add("block.acervus.item_heap.holding", "%s x %s");
            add("block.acervus.fluid_heap.holding", "%s, %s");
            add("block.acervus.energy_heap.holding", "%s FE");
            add("block.acervus.chemical_heap.holding", "%s, %s");
            add("block.acervus.item_heap.absorbing", "Collecting what you pick up");
            if (Mods.mekanism()) {
                add(io.github.capsicum0907.acervus.gas.GasHeap.BLOCK.get(), "Gas Heap");
            }
            add("gui.acervus.room", "%s more will fit");
            add("gui.acervus.vessel", "Put a container here");
            add("gui.acervus.flow.in", "In");
            add("gui.acervus.flow.out", "Out");
            add("gui.acervus.exact", "%s stored");
            add("gui.acervus.intake_only", "Deposit only");
            add("gui.acervus.energy", "Energy");
            add("block.acervus.horreum.holding", "%s of %s heaps");
        }
    }

    /**
     * A chest you can see into, held together with iron. Deliberately not gated
     * behind anything rare: how much a heap holds is a setting, so the recipe is
     * about when it becomes available, not about how strong it is.
     */
    private static class Recipes extends RecipeProvider {
        Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, AcervusRegistry.HEAP.get())
                    .pattern("GIG")
                    .pattern("ICI")
                    .pattern("GIG")
                    .define('G', Blocks.GLASS)
                    .define('I', Items.IRON_INGOT)
                    .define('C', Blocks.CHEST)
                    .unlockedBy("has_chest", has(Blocks.CHEST))
                    .save(output);

            // The same frame around a cauldron instead of a chest: the pair should read
            // as one machine holding two kinds of thing.
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, AcervusRegistry.FLUID_HEAP.get())
                    .pattern("GIG")
                    .pattern("ICI")
                    .pattern("GIG")
                    .define('G', Blocks.GLASS)
                    .define('I', Items.IRON_INGOT)
                    .define('C', Blocks.CAULDRON)
                    .unlockedBy("has_cauldron", has(Blocks.CAULDRON))
                    .save(output);

            // A frame of iron around a chest, with no glass: a rack is the one block of
            // the set you cannot see into, and it costs what a heap costs to make.
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, AcervusRegistry.HORREUM.get())
                    .pattern("III")
                    .pattern("ICI")
                    .pattern("III")
                    .define('I', Items.IRON_INGOT)
                    .define('C', Blocks.CHEST)
                    .unlockedBy("has_chest", has(Blocks.CHEST))
                    .save(output);

            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, AcervusRegistry.ENERGY_HEAP.get())
                    .pattern("GIG")
                    .pattern("ICI")
                    .pattern("GIG")
                    .define('G', Blocks.GLASS)
                    .define('I', Items.IRON_INGOT)
                    .define('C', Blocks.REDSTONE_BLOCK)
                    .unlockedBy("has_redstone_block", has(Blocks.REDSTONE_BLOCK))
                    .save(output);

            // A bottle rather than a cauldron: the same ingredients cannot make two
            // different blocks, and a bottle is the vanilla thing that holds a vapour.
            if (Mods.mekanism()) {
                ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS,
                                io.github.capsicum0907.acervus.gas.GasHeap.BLOCK.get())
                        .pattern("GIG")
                        .pattern("ICI")
                        .pattern("GIG")
                        .define('G', Blocks.GLASS)
                        .define('I', Items.IRON_INGOT)
                        .define('C', Items.GLASS_BOTTLE)
                        .unlockedBy("has_glass_bottle", has(Items.GLASS_BOTTLE))
                        .save(output);
            }
        }
    }
}
