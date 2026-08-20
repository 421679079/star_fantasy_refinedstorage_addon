package com.starfantasy.refinedstorageaddon.compat.terracurio;

import com.starfantasy.refinedstorageaddon.station.NetworkMenuSession;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class TerraCurioStationCompat {
    private TerraCurioStationCompat() {
    }

    public static AbstractContainerMenu createMenu(int containerId, Inventory inventory,
                                                   NetworkMenuSession session) {
        return new NetworkWorkshopMenu(containerId, inventory, session);
    }

    public static void selectRecipe(AbstractContainerMenu menu, Player player, ItemStack expected) {
        if (menu instanceof NetworkWorkshopMenu workshop) {
            workshop.selectRecipe(player, expected);
        }
    }
}
