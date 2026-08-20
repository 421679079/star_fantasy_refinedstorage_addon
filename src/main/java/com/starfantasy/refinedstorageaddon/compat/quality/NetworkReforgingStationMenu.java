package com.starfantasy.refinedstorageaddon.compat.quality;

import com.starfantasy.refinedstorageaddon.station.NetworkMenuSession;
import com.starfantasy.refinedstorageaddon.station.NetworkStationMenu;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.qualityequipment.world.inventory.ReforgingStationGUIMenu;

public final class NetworkReforgingStationMenu extends ReforgingStationGUIMenu
        implements NetworkStationMenu {
    private final NetworkMenuSession session;
    private boolean lastResultPresent;
    private int lastMaterialCount;
    private int refillWindowUntilTick = -1;

    public NetworkReforgingStationMenu(int containerId, Inventory inventory, FriendlyByteBuf data,
                                       NetworkMenuSession session) {
        super(containerId, inventory, data);
        this.session = session;
        this.lastResultPresent = getSlot(StationKind.QUALITY_REFORGING_STATION.resultSlot())
                .hasItem();
        this.lastMaterialCount = materialCount();
    }

    @Override
    public NetworkMenuSession starFantasySession() {
        return session;
    }

    @Override
    public StationKind starFantasyKind() {
        return StationKind.QUALITY_REFORGING_STATION;
    }

    @Override
    public boolean stillValid(Player player) {
        return session.isUsable(player);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (session == null || !(entity instanceof ServerPlayer serverPlayer)) {
            return;
        }

        boolean resultPresent = getSlot(StationKind.QUALITY_REFORGING_STATION.resultSlot())
                .hasItem();
        int materialCount = materialCount();
        int currentTick = serverPlayer.getServer().getTickCount();

        if (serverPlayer.containerMenu != this) {
            refillWindowUntilTick = -1;
        } else {
            if (materialCount > 0) {
                session.rememberInput(this, 1, 1);
            }
            if (lastResultPresent && !resultPresent) {
                refillWindowUntilTick = currentTick + 2;
            }
            if (materialCount < lastMaterialCount) {
                boolean consumedByReforge = currentTick <= refillWindowUntilTick;
                refillWindowUntilTick = -1;
                if (consumedByReforge && materialCount == 0) {
                    lastResultPresent = resultPresent;
                    lastMaterialCount = materialCount;
                    session.refillEmpty(this);
                    materialCount = materialCount();
                    resultPresent = getSlot(
                            StationKind.QUALITY_REFORGING_STATION.resultSlot()).hasItem();
                }
            } else if (currentTick > refillWindowUntilTick) {
                refillWindowUntilTick = -1;
            }
        }

        lastResultPresent = resultPresent;
        lastMaterialCount = materialCount;
    }

    @Override
    public void removed(Player player) {
        refillWindowUntilTick = -1;
        session.returnInputs(this);
        super.removed(player);
        session.returnToGridAfterClose();
    }

    private int materialCount() {
        return getSlot(StationKind.QUALITY_REFORGING_STATION.inputSlot(1))
                .getItem().getCount();
    }
}
