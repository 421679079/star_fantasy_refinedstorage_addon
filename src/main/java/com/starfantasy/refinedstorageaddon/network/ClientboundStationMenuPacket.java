package com.starfantasy.refinedstorageaddon.network;

import com.starfantasy.refinedstorageaddon.client.ClientPacketHandler;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientboundStationMenuPacket(int containerId, StationKind kind) {
    public static void encode(ClientboundStationMenuPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeVarInt(packet.kind.ordinal());
    }

    public static ClientboundStationMenuPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundStationMenuPacket(buffer.readVarInt(), StationKind.byId(buffer.readVarInt()));
    }

    public static void handle(ClientboundStationMenuPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (packet.kind != null) {
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientPacketHandler.markNetworkMenu(packet.containerId, packet.kind)));
        }
        context.setPacketHandled(true);
    }
}
