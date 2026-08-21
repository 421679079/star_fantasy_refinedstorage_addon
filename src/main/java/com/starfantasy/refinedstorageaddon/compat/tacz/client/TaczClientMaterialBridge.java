package com.starfantasy.refinedstorageaddon.compat.tacz.client;

import com.starfantasy.refinedstorageaddon.client.ClientStationState;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.util.List;

public final class TaczClientMaterialBridge {
    private static final Field SELECTED_RECIPE = field("selectedRecipe");
    private static final Field INGREDIENT_COUNTS = field("playerIngredientCount");
    private static ResourceLocation lastRecipeId;
    private static long lastNetworkRevision = Long.MIN_VALUE;
    private static int lastInventorySignature;

    private TaczClientMaterialBridge() {
    }

    public static void clientTick(Screen screen) {
        if (!(screen instanceof GunSmithTableScreen gunSmithScreen)
                || !ClientStationState.isTaczNetworkMenu(gunSmithScreen.getMenu())) {
            reset();
            return;
        }
        if (SELECTED_RECIPE == null || INGREDIENT_COUNTS == null) {
            return;
        }
        try {
            GunSmithTableRecipe recipe = (GunSmithTableRecipe) SELECTED_RECIPE.get(gunSmithScreen);
            if (recipe == null) {
                return;
            }
            ResourceLocation recipeId = recipe.getId();
            long networkRevision = ClientStationState.materialCacheRevision();
            int inventorySignature = inventorySignature();
            if (recipeId.equals(lastRecipeId)
                    && networkRevision == lastNetworkRevision
                    && inventorySignature == lastInventorySignature) {
                return;
            }

            List<GunSmithTableIngredient> ingredients = recipe.getInputs();
            Int2IntArrayMap counts = new Int2IntArrayMap(ingredients.size());
            for (int index = 0; index < ingredients.size(); index++) {
                long available = ClientStationState.countAvailable(
                        ingredients.get(index).getIngredient());
                counts.put(index, (int) Math.min(Integer.MAX_VALUE, available));
            }
            INGREDIENT_COUNTS.set(gunSmithScreen, counts);
            lastRecipeId = recipeId;
            lastNetworkRevision = networkRevision;
            lastInventorySignature = inventorySignature;
        } catch (IllegalAccessException ignored) {
            reset();
        }
    }

    private static int inventorySignature() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return 0;
        }
        int result = 1;
        for (ItemStack stack : minecraft.player.getInventory().items) {
            result = 31 * result + Item.getId(stack.getItem());
            result = 31 * result + (stack.hasTag() ? stack.getTag().hashCode() : 0);
            result = 31 * result + stack.getCount();
        }
        return result;
    }

    private static Field field(String name) {
        try {
            Field field = GunSmithTableScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void reset() {
        lastRecipeId = null;
        lastNetworkRevision = Long.MIN_VALUE;
        lastInventorySignature = 0;
    }
}
