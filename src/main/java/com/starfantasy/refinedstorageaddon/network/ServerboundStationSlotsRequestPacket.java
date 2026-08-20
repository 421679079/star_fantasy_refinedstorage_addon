package com.starfantasy.refinedstorageaddon.network;

import com.starfantasy.refinedstorageaddon.station.StationSlotService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ServerboundStationSlotsRequestPacket(int containerId) {
    public static void encode(ServerboundStationSlotsRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
    }

    public static ServerboundStationSlotsRequestPacket decode(FriendlyByteBuf buffer) {
        return new ServerboundStationSlotsRequestPacket(buffer.readVarInt());
    }

    public static void handle(ServerboundStationSlotsRequestPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                StationSlotService.sync(player, packet.containerId);
            }
        });
        context.setPacketHandled(true);
    }
}
