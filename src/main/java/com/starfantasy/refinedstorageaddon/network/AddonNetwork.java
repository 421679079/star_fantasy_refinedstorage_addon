package com.starfantasy.refinedstorageaddon.network;

import com.starfantasy.refinedstorageaddon.StarFantasyRefinedStorageAddon;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class AddonNetwork {
    private static final String PROTOCOL = "9";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(StarFantasyRefinedStorageAddon.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private AddonNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, ServerboundStationSlotsRequestPacket.class,
                ServerboundStationSlotsRequestPacket::encode,
                ServerboundStationSlotsRequestPacket::decode,
                ServerboundStationSlotsRequestPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, ServerboundStationSlotClickPacket.class,
                ServerboundStationSlotClickPacket::encode,
                ServerboundStationSlotClickPacket::decode,
                ServerboundStationSlotClickPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, ClientboundStationSlotsPacket.class,
                ClientboundStationSlotsPacket::encode,
                ClientboundStationSlotsPacket::decode,
                ClientboundStationSlotsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, ServerboundOpenStationPacket.class,
                ServerboundOpenStationPacket::encode,
                ServerboundOpenStationPacket::decode,
                ServerboundOpenStationPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, ClientboundStationMenuPacket.class,
                ClientboundStationMenuPacket::encode,
                ClientboundStationMenuPacket::decode,
                ClientboundStationMenuPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, ServerboundReturnToGridPacket.class,
                ServerboundReturnToGridPacket::encode,
                ServerboundReturnToGridPacket::decode,
                ServerboundReturnToGridPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, ServerboundSelectIronsScrollSpellPacket.class,
                ServerboundSelectIronsScrollSpellPacket::encode,
                ServerboundSelectIronsScrollSpellPacket::decode,
                ServerboundSelectIronsScrollSpellPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, ServerboundTakeIronsScrollResultPacket.class,
                ServerboundTakeIronsScrollResultPacket::encode,
                ServerboundTakeIronsScrollResultPacket::decode,
                ServerboundTakeIronsScrollResultPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id, ClientboundTaczNetworkConsumptionPacket.class,
                ClientboundTaczNetworkConsumptionPacket::encode,
                ClientboundTaczNetworkConsumptionPacket::decode,
                ClientboundTaczNetworkConsumptionPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
