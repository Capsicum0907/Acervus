package io.github.capsicum0907.acervus.data;

import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.tags.ItemTags;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentPredicate;
import java.util.concurrent.CompletableFuture;

import io.github.capsicum0907.acervus.Acervus;
import io.github.capsicum0907.acervus.AcervusRegistry;
import io.github.capsicum0907.acervus.EnergyHeapBlock;

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
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredBlock;

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

    private static class Tags extends BlockTagsProvider {
        Tags(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                ExistingFileHelper existingFileHelper) {
            super(output, registries, Acervus.MODID, existingFileHelper);
        }

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
            drawnItem(AcervusRegistry.FLUID_HEAP);
            energyBox();
            String rack = AcervusRegistry.HORREUM.getId().getPath();
            ResourceLocation casing = modLoc("block/" + AcervusRegistry.ENERGY_HEAP.getId().getPath() + "_side");
            ModelFile rackModel = models().orientable(rack, casing, modLoc("block/" + rack + "_front"), casing);
            horizontalBlock(AcervusRegistry.HORREUM.get(), rackModel);
            itemModels().withExistingParent(rack, modLoc("block/" + rack));
            glassBox(io.github.capsicum0907.acervus.gas.GasHeap.BLOCK);
            drawnItem(io.github.capsicum0907.acervus.gas.GasHeap.BLOCK);
        }

        private void drawnItem(DeferredBlock<?> block) {
            itemModels().getBuilder(block.getId().getPath())
                    .parent(new ModelFile.UncheckedModelFile("minecraft:builtin/entity"))
                    .transforms()
                    .transform(ItemDisplayContext.GUI).rotation(30, 225, 0).scale(0.625F).end()
                    .transform(ItemDisplayContext.GROUND).translation(0, 3, 0).scale(0.25F).end()
                    .transform(ItemDisplayContext.FIXED).scale(0.5F).end()
                    .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(75, 45, 0)
                    .translation(0, 2.5F, 0).scale(0.375F).end()
                    .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 45, 0).scale(0.4F).end()
                    .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 225, 0).scale(0.4F).end()
                    .end();
        }

        private void energyBox() {
            String name = AcervusRegistry.ENERGY_HEAP.getId().getPath();
            ResourceLocation side = modLoc("block/" + name + "_side");
            ModelFile[] lit = new ModelFile[EnergyHeapBlock.MAX_LAMPS + 1];
            for (int lamps = 0; lamps < lit.length; lamps++) {
                lit[lamps] = models().orientable(name + "_" + lamps, side,
                        modLoc("block/" + name + "_front_" + lamps), side);
            }
            getVariantBuilder(AcervusRegistry.ENERGY_HEAP.get()).forAllStates(state -> ConfiguredModel.builder()
                    .modelFile(lit[state.getValue(EnergyHeapBlock.LAMPS)])
                    .rotationY(((int) state.getValue(EnergyHeapBlock.FACING).toYRot() + 180) % 360)
                    .build());
            ItemModelBuilder item = itemModels().withExistingParent(name, modLoc("block/" + name + "_0"));
            for (int lamps = 1; lamps < lit.length; lamps++) {
                item.override()
                        .predicate(modLoc(EnergyHeapBlock.LAMPS_PROPERTY), EnergyHeapBlock.lampFraction(lamps))
                        .model(lit[lamps])
                        .end();
            }
        }

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
            add(AcervusRegistry.HEAP.get(), "Item Heap");
            add(AcervusRegistry.FLUID_HEAP.get(), "Fluid Heap");
            add(AcervusRegistry.ENERGY_HEAP.get(), "Energy Heap");
            add(AcervusRegistry.HORREUM.get(), "Horreum");
            add("gui.acervus.empty", "Empty");
            add("block.acervus.item_heap.holding", "%s x %s");
            add("block.acervus.fluid_heap.holding", "%s, %s");
            add("block.acervus.energy_heap.holding", "%s FE");
            add("block.acervus.chemical_heap.holding", "%s, %s");
            add("block.acervus.item_heap.absorbing", "Collecting what you pick up");
            add(io.github.capsicum0907.acervus.gas.GasHeap.BLOCK.get(), "Gas Heap");
            add("gui.acervus.room", "%s more will fit");
            add("gui.acervus.vessel", "Put a container here");
            add("gui.acervus.flow.in", "In");
            add("gui.acervus.flow.out", "Out");
            add("gui.acervus.of", "%s / %s");
            add("gui.acervus.locked", "Locked");
            add("gui.acervus.free", "Free");
            add("gui.acervus.switch_form", "Click to switch what comes out");
            add("gui.acervus.scale", "%s - click to change");
            add("gui.acervus.scale.linear", "Linear");
            add("gui.acervus.scale.log", "Logarithmic");
            add("gui.acervus.scale.decade", "Within the decade");
            add("gui.acervus.exact", "%s stored");
            add("gui.acervus.intake_only", "Deposit only");
            add("gui.acervus.energy", "Energy");
            add("block.acervus.horreum.holding", "%s of %s heaps");
            add("acervus.stacked.open", "One at a time: take a single one out of the stack.");
            add("acervus.stacked.place", "This stack is holding something and cannot be placed.");
        }
    }

    private static class Recipes extends RecipeProvider {
        Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            heap(output, AcervusRegistry.HEAP.get(), Ingredient.of(Blocks.CHEST));
            heap(output, AcervusRegistry.FLUID_HEAP.get(), Ingredient.of(Blocks.CAULDRON));
            heap(output, AcervusRegistry.ENERGY_HEAP.get(), Ingredient.of(Blocks.REDSTONE_BLOCK));
            rack(output, AcervusRegistry.HEAP_ITEM.get());
            rack(output, AcervusRegistry.FLUID_HEAP_ITEM.get());
            rack(output, AcervusRegistry.ENERGY_HEAP_ITEM.get());

            heap(output.withConditions(new ModLoadedCondition(MEKANISM)), io.github.capsicum0907.acervus.gas.GasHeap.BLOCK.get(),
                    Ingredient.of(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/osmium"))));
            rack(output, io.github.capsicum0907.acervus.gas.GasHeap.ITEM.get());
        }

        private static final String MEKANISM = "mekanism";

        private static void heap(RecipeOutput output, ItemLike heap, Ingredient centre) {
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, heap)
                    .pattern("ISI")
                    .pattern("NCN")
                    .pattern("ISI")
                    .define('I', Blocks.IRON_BLOCK)
                    .define('S', Items.NETHER_STAR)
                    .define('N', Items.NETHERITE_INGOT)
                    .define('C', centre)
                    .unlockedBy("has_nether_star", has(Items.NETHER_STAR))
                    .save(output);
        }

        private static void rack(RecipeOutput output, Item heap) {
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, AcervusRegistry.HORREUM.get())
                    .pattern("NSN")
                    .pattern("SHS")
                    .pattern("NSN")
                    .define('N', Items.NETHERITE_INGOT)
                    .define('S', Items.NETHER_STAR)
                    .define('H', DataComponentIngredient.of(true, DataComponentPredicate.EMPTY, heap))
                    .unlockedBy("has_heap", has(heap))
                    .save(output, ResourceLocation.fromNamespaceAndPath(Acervus.MODID,
                            "horreum_from_" + BuiltInRegistries.ITEM.getKey(heap).getPath()));
        }
    }
}
