package com.starfantasy.refinedstorageaddon.station.menu;

import com.starfantasy.refinedstorageaddon.station.NetworkMenuSession;
import com.starfantasy.refinedstorageaddon.station.NetworkStationMenu;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.StonecutterMenu;

public final class NetworkStonecutterMenu extends StonecutterMenu implements NetworkStationMenu {
    private final NetworkMenuSession session;

    public NetworkStonecutterMenu(int containerId, Inventory inventory, NetworkMenuSession session) {
        super(containerId, inventory, session.levelAccess());
        this.session = session;
    }

    @Override
    public NetworkMenuSession starFantasySession() {
        return session;
    }

    @Override
    public StationKind starFantasyKind() {
        return StationKind.STONECUTTER;
    }

    @Override
    public boolean stillValid(Player player) {
        return session.isUsable(player);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        boolean tookResult = slotId == StationKind.STONECUTTER.resultSlot() && getSlot(slotId).hasItem();
        int selectedRecipe = tookResult ? getSelectedRecipeIndex() : -1;
        super.clicked(slotId, button, clickType, player);
        if (tookResult) {
            session.refillEmpty(this);
            if (selectedRecipe >= 0 && hasInputItem()) {
                clickMenuButton(player, selectedRecipe);
            }
        }
    }

    @Override
    public void removed(Player player) {
        session.returnInputs(this);
        super.removed(player);
        session.returnToGridAfterClose();
    }
}
