package com.starfantasy.refinedstorageaddon.network;

import com.starfantasy.refinedstorageaddon.station.NetworkStationMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Requests a server-driven workstation-to-grid transition. */
public record ServerboundReturnToGridPacket() {
    public static void encode(ServerboundReturnToGridPacket packet, FriendlyByteBuf buffer) {
    }

    public static ServerboundReturnToGridPacket decode(FriendlyByteBuf buffer) {
        return new ServerboundReturnToGridPacket();
    }

    public static void handle(ServerboundReturnToGridPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof NetworkStationMenu stationMenu) {
                stationMenu.starFantasySession().returnToGridNow(player.containerMenu);
            }
        });
        context.setPacketHandled(true);
    }
}
