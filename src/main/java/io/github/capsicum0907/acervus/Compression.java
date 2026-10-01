package io.github.capsicum0907.acervus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class Compression {
    private static final int[] SIDES = { 2, 3 };

    public record Form(Item item, long factor) {
    }

    private record Link(Item other, int ratio) {
    }

    private static volatile Map<Item, Link> up = Map.of();
    private static volatile Map<Item, Link> down = Map.of();
    private static volatile boolean ready;

    private Compression() {
    }

    public static boolean ready() {
        return ready;
    }

    public static long unitOf(Item item) {
        for (Form form : chainOf(item)) {
            if (form.item() == item) {
                return form.factor();
            }
        }
        return 1L;
    }

    public static long rebase(long amount, long from, long to) {
        if (from == to || from <= 0L || to <= 0L) {
            return amount;
        }
        if (to % from == 0L) {
            long by = to / from;
            return amount > Long.MAX_VALUE / by ? Long.MAX_VALUE : amount * by;
        }
        if (from % to == 0L) {
            return amount / (from / to);
        }
        long whole = amount / from;
        return whole > Long.MAX_VALUE / to ? Long.MAX_VALUE : whole * to;
    }

    public static long times(long value, long by) {
        return by > 0L && value > Long.MAX_VALUE / by ? Long.MAX_VALUE : value * by;
    }

    public static List<Form> chainOf(Item item) {
        Item smallest = item;
        Set<Item> seen = new HashSet<>();
        while (down.containsKey(smallest) && seen.add(smallest)) {
            smallest = down.get(smallest).other();
        }
        List<Form> chain = new ArrayList<>();
        long factor = 1L;
        Item current = smallest;
        seen.clear();
        while (current != null && seen.add(current)) {
            chain.add(new Form(current, factor));
            Link next = up.get(current);
            if (next == null) {
                break;
            }
            factor = saturatingTimes(factor, next.ratio());
            current = next.other();
        }
        return Collections.unmodifiableList(chain);
    }

    public static void rebuild(RecipeManager recipes, Level level) {
        long started = System.nanoTime();
        Map<Item, Link> foundUp = new HashMap<>();
        Map<Item, Link> foundDown = new HashMap<>();
        Set<Item> ambiguous = new HashSet<>();
        for (RecipeHolder<CraftingRecipe> holder : recipes.getAllRecipesFor(RecipeType.CRAFTING)) {
            for (Item small : candidates(holder.value())) {
                for (int side : SIDES) {
                    Optional<Item> large = compress(recipes, level, small, side);
                    if (large.isEmpty() || !expands(recipes, level, large.get(), small, side * side)) {
                        continue;
                    }
                    record(foundUp, ambiguous, small, new Link(large.get(), side * side));
                    record(foundDown, ambiguous, large.get(), new Link(small, side * side));
                }
            }
        }
        foundUp.keySet().removeAll(ambiguous);
        foundDown.keySet().removeAll(ambiguous);
        foundUp.entrySet().removeIf(entry -> ambiguous.contains(entry.getValue().other()));
        foundDown.entrySet().removeIf(entry -> ambiguous.contains(entry.getValue().other()));
        up = Map.copyOf(foundUp);
        down = Map.copyOf(foundDown);
        ready = true;
        Acervus.LOGGER.info("Found {} free conversions in {} ms", up.size(), (System.nanoTime() - started) / 1_000_000L);
    }

    private static Set<Item> candidates(CraftingRecipe recipe) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        List<Ingredient> used = ingredients.stream().filter(ingredient -> !ingredient.isEmpty()).toList();
        if (used.size() != 4 && used.size() != 9) {
            return Set.of();
        }
        Set<Item> common = null;
        for (Ingredient ingredient : used) {
            Set<Item> items = new HashSet<>();
            for (ItemStack stack : ingredient.getItems()) {
                items.add(stack.getItem());
            }
            if (common == null) {
                common = items;
            } else {
                common.retainAll(items);
            }
        }
        return common == null ? Set.of() : common;
    }

    private static Optional<Item> compress(RecipeManager recipes, Level level, Item small, int side) {
        List<ItemStack> grid = new ArrayList<>();
        for (int slot = 0; slot < side * side; slot++) {
            grid.add(new ItemStack(small));
        }
        ItemStack result = craft(recipes, level, CraftingInput.of(side, side, grid));
        if (result.getCount() != 1 || result.is(small) || !result.getComponentsPatch().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.getItem());
    }

    private static boolean expands(RecipeManager recipes, Level level, Item large, Item small, int ratio) {
        ItemStack result = craft(recipes, level, CraftingInput.of(1, 1, List.of(new ItemStack(large))));
        return result.is(small) && result.getCount() == ratio && result.getComponentsPatch().isEmpty();
    }

    private static ItemStack craft(RecipeManager recipes, Level level, CraftingInput input) {
        try {
            return recipes.getRecipeFor(RecipeType.CRAFTING, input, level)
                    .map(found -> found.value().assemble(input, level.registryAccess()))
                    .orElse(ItemStack.EMPTY);
        } catch (RuntimeException refused) {
            return ItemStack.EMPTY;
        }
    }

    private static void record(Map<Item, Link> found, Set<Item> ambiguous, Item from, Link link) {
        Link existing = found.putIfAbsent(from, link);
        if (existing != null && !existing.equals(link)) {
            ambiguous.add(from);
        }
    }

    private static long saturatingTimes(long value, int by) {
        return value > Long.MAX_VALUE / by ? Long.MAX_VALUE : value * by;
    }
}
