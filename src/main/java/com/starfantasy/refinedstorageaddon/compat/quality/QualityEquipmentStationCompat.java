package com.starfantasy.refinedstorageaddon.compat.quality;

import com.starfantasy.refinedstorageaddon.station.NetworkMenuSession;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class QualityEquipmentStationCompat {
    private QualityEquipmentStationCompat() {
    }

    public static AbstractContainerMenu createMenu(int containerId, Inventory inventory,
                                                   NetworkMenuSession session) {
        FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer());
        data.writeBlockPos(session.virtualMenuPosition());
        return new NetworkReforgingStationMenu(containerId, inventory, data, session);
    }
}
