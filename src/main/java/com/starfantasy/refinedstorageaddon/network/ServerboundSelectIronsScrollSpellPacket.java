package com.starfantasy.refinedstorageaddon.network;

import com.starfantasy.refinedstorageaddon.compat.irons.IronsSpellbooksStationCompat;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ServerboundSelectIronsScrollSpellPacket(String spellId) {
    private static final int MAX_SPELL_ID_LENGTH = 256;

    public static void encode(ServerboundSelectIronsScrollSpellPacket packet,
                              FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.spellId, MAX_SPELL_ID_LENGTH);
    }

    public static ServerboundSelectIronsScrollSpellPacket decode(FriendlyByteBuf buffer) {
        return new ServerboundSelectIronsScrollSpellPacket(
                buffer.readUtf(MAX_SPELL_ID_LENGTH));
    }

    public static void handle(ServerboundSelectIronsScrollSpellPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && !packet.spellId.isBlank()
                    && StationKind.IRONS_SCROLL_FORGE.isInstalled()) {
                IronsSpellbooksStationCompat.selectScrollSpell(player, packet.spellId);
            }
        });
        context.setPacketHandled(true);
    }
}
