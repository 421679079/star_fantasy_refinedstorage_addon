package com.starfantasy.refinedstorageaddon.network;

import com.starfantasy.refinedstorageaddon.client.ClientPacketHandler;
import com.starfantasy.refinedstorageaddon.station.StationSlotStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record ClientboundStationSlotsPacket(int containerId, List<ItemStack> slots) {
    public ClientboundStationSlotsPacket {
        slots = List.copyOf(slots);
    }

    public static void encode(ClientboundStationSlotsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeVarInt(Math.min(packet.slots.size(), StationSlotStorage.SLOT_COUNT));
        packet.slots.stream().limit(StationSlotStorage.SLOT_COUNT).forEach(buffer::writeItem);
    }

    public static ClientboundStationSlotsPacket decode(FriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        int count = buffer.readVarInt();
        if (count < 0 || count > StationSlotStorage.SLOT_COUNT) {
            throw new IllegalArgumentException("Invalid workstation slot count: " + count);
        }
        List<ItemStack> slots = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            slots.add(buffer.readItem());
        }
        while (slots.size() < StationSlotStorage.SLOT_COUNT) {
            slots.add(ItemStack.EMPTY);
        }
        return new ClientboundStationSlotsPacket(containerId, slots);
    }

    public static void handle(ClientboundStationSlotsPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.applyStationSlots(packet.containerId, packet.slots)));
        context.setPacketHandled(true);
    }
}
