package com.starfantasy.refinedstorageaddon.network;

import com.starfantasy.refinedstorageaddon.compat.irons.IronsSpellbooksStationCompat;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ServerboundTakeIronsScrollResultPacket(String spellId, boolean quickMove) {
    private static final int MAX_SPELL_ID_LENGTH = 256;

    public static void encode(ServerboundTakeIronsScrollResultPacket packet,
                              FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.spellId, MAX_SPELL_ID_LENGTH);
        buffer.writeBoolean(packet.quickMove);
    }

    public static ServerboundTakeIronsScrollResultPacket decode(FriendlyByteBuf buffer) {
        return new ServerboundTakeIronsScrollResultPacket(
                buffer.readUtf(MAX_SPELL_ID_LENGTH), buffer.readBoolean());
    }

    public static void handle(ServerboundTakeIronsScrollResultPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && !packet.spellId.isBlank()
                    && StationKind.IRONS_SCROLL_FORGE.isInstalled()) {
                IronsSpellbooksStationCompat.takeScrollResult(
                        player, packet.spellId, packet.quickMove);
            }
        });
        context.setPacketHandled(true);
    }
}
