package com.starfantasy.refinedstorageaddon.compat.tacz;

import com.starfantasy.refinedstorageaddon.station.NetworkMenuSession;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class TaczStationCompat {
    private static final ResourceLocation GUN_SMITH_TABLE = id("tacz:gun_smith_table");
    private static final ResourceLocation AMMO_WORKBENCH = id("tacz:ammo_workbench");
    private static final ResourceLocation ATTACHMENT_WORKBENCH = id("tacz:attachment_workbench");

    private TaczStationCompat() {
    }

    public static void openMenu(ServerPlayer player, NetworkMenuSession session,
                                StationKind kind, Component title) {
        ResourceLocation blockId = blockId(kind);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new NetworkGunSmithTableMenu(
                        containerId, inventory, blockId, session, kind), title),
                buffer -> buffer.writeResourceLocation(blockId));
    }

    private static ResourceLocation blockId(StationKind kind) {
        return switch (kind) {
            case TACZ_GUN_SMITH_TABLE -> GUN_SMITH_TABLE;
            case TACZ_AMMO_WORKBENCH -> AMMO_WORKBENCH;
            case TACZ_ATTACHMENT_WORKBENCH -> ATTACHMENT_WORKBENCH;
            default -> throw new IllegalArgumentException("Not a TACZ workstation: " + kind);
        };
    }

    private static ResourceLocation id(String value) {
        ResourceLocation result = ResourceLocation.tryParse(value);
        if (result == null) {
            throw new IllegalArgumentException("Invalid TACZ resource location: " + value);
        }
        return result;
    }
}
