package com.starfantasy.refinedstorageaddon.station.menu;

import com.starfantasy.refinedstorageaddon.station.NetworkMenuSession;
import com.starfantasy.refinedstorageaddon.station.NetworkStationMenu;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.SmithingMenu;

public final class NetworkSmithingMenu extends SmithingMenu implements NetworkStationMenu {
    private final NetworkMenuSession session;

    public NetworkSmithingMenu(int containerId, Inventory inventory, NetworkMenuSession session) {
        super(containerId, inventory, session.levelAccess());
        this.session = session;
    }

    @Override
    public NetworkMenuSession starFantasySession() {
        return session;
    }

    @Override
    public StationKind starFantasyKind() {
        return StationKind.SMITHING;
    }

    @Override
    public boolean stillValid(Player player) {
        return session.isUsable(player);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        boolean tookResult = slotId == StationKind.SMITHING.resultSlot() && getSlot(slotId).hasItem();
        super.clicked(slotId, button, clickType, player);
        if (tookResult) {
            session.refillEmpty(this);
        }
    }

    @Override
    public void removed(Player player) {
        session.returnInputs(this);
        super.removed(player);
        session.returnToGridAfterClose();
    }
}
