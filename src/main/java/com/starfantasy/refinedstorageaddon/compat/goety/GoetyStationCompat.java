package com.starfantasy.refinedstorageaddon.compat.goety;

import com.starfantasy.refinedstorageaddon.station.NetworkMenuSession;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class GoetyStationCompat {
    private GoetyStationCompat() {
    }

    public static AbstractContainerMenu createDarkAnvilMenu(int containerId, Inventory inventory,
                                                             NetworkMenuSession session) {
        return new NetworkDarkAnvilMenu(containerId, inventory, session);
    }
}
