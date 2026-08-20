package com.starfantasy.refinedstorageaddon.network;

import com.starfantasy.refinedstorageaddon.station.StationSlotService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ServerboundStationSlotClickPacket(int containerId, int slotIndex, int mouseButton) {
    public static void encode(ServerboundStationSlotClickPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeVarInt(packet.slotIndex);
        buffer.writeVarInt(packet.mouseButton);
    }

    public static ServerboundStationSlotClickPacket decode(FriendlyByteBuf buffer) {
        return new ServerboundStationSlotClickPacket(buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt());
    }

    public static void handle(ServerboundStationSlotClickPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                StationSlotService.click(player, packet.containerId, packet.slotIndex,
                        packet.mouseButton);
            }
        });
        context.setPacketHandled(true);
    }
}
