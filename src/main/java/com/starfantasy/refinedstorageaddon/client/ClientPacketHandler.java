package com.starfantasy.refinedstorageaddon.client;

import com.starfantasy.refinedstorageaddon.network.ClientboundTaczNetworkConsumptionPacket.ConsumedStack;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void applyStationSlots(int containerId, List<ItemStack> slots) {
        ClientStationState.applyStationSlots(containerId, slots);
    }

    public static void markNetworkMenu(int containerId, StationKind kind) {
        ClientStationState.markNetworkMenu(containerId, kind);
    }

    public static void consumeTaczNetworkMaterials(List<ConsumedStack> consumed) {
        ClientStationState.consumeNetworkMaterials(consumed);
    }
}
