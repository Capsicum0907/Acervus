package io.github.capsicum0907.acervus.data;

import java.util.concurrent.CompletableFuture;

import io.github.capsicum0907.acervus.Acervus;
import io.github.capsicum0907.acervus.AcervusRegistry;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
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
    }

    private static class Models extends BlockStateProvider {
        Models(PackOutput output, ExistingFileHelper existingFileHelper) {
            super(output, Acervus.MODID, existingFileHelper);
        }

        @Override
        protected void registerStatesAndModels() {
            DeferredBlock<?> heap = AcervusRegistry.HEAP;
            String name = heap.getId().getPath();

            // translucent, because the middle of the texture is see-through and the
            // default render type would draw those pixels as fully opaque.
            ModelFile model = models()
                    .cubeAll(name, modLoc("block/" + name))
                    .renderType("minecraft:translucent");

            simpleBlock(heap.get(), model);
            itemModels().withExistingParent(name, modLoc("block/" + name));
        }
    }

    private static class Language extends LanguageProvider {
        Language(PackOutput output) {
            super(output, Acervus.MODID, "en_us");
        }

        @Override
        protected void addTranslations() {
            add(AcervusRegistry.HEAP.get(), "Heap");
            add("block.acervus.heap.empty", "Empty");
            add("block.acervus.heap.holding", "%s x %s");
            add("gui.acervus.room", "%s more will fit");
            add("gui.acervus.exact", "%s stored");
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
        }
    }
}
