package com.starfantasy.refinedstorageaddon.compat.transmog;

import com.starfantasy.refinedstorageaddon.station.NetworkMenuSession;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;

public final class TransmogStationCompat {
    private TransmogStationCompat() {
    }

    public static AbstractContainerMenu createMenu(int containerId, Inventory inventory,
                                                   NetworkMenuSession session) {
        return new NetworkTransmogMenu(containerId, inventory, session);
    }

    static ContainerData infiniteFuel() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? 3 : 0;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
    }
}
