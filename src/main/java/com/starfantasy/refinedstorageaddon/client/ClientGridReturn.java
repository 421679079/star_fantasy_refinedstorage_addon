package com.starfantasy.refinedstorageaddon.client;

import com.starfantasy.refinedstorageaddon.network.AddonNetwork;
import com.starfantasy.refinedstorageaddon.network.ServerboundReturnToGridPacket;
import net.minecraft.client.Minecraft;

/** Keeps closing a network workstation as a server-authoritative menu transition. */
public final class ClientGridReturn {
    private ClientGridReturn() {
    }

    public static boolean request() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            return false;
        }
        AddonNetwork.CHANNEL.sendToServer(new ServerboundReturnToGridPacket());
        return true;
    }
}
