package com.starfantasy.refinedstorageaddon.compat.irons;

import com.starfantasy.refinedstorageaddon.registry.AddonMenus;
import com.starfantasy.refinedstorageaddon.station.NetworkMenuSession;
import com.starfantasy.refinedstorageaddon.station.NetworkStationMenu;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class NetworkArcaneAnvilMenu extends ArcaneAnvilMenu implements NetworkStationMenu {
    @Nullable
    private final NetworkMenuSession session;

    public NetworkArcaneAnvilMenu(int containerId, Inventory inventory) {
        super(containerId, inventory, ContainerLevelAccess.NULL);
        this.session = null;
    }

    public NetworkArcaneAnvilMenu(int containerId, Inventory inventory,
                                  NetworkMenuSession session) {
        super(containerId, inventory, session.levelAccess());
        this.session = session;
    }

    @Override
    public MenuType<?> getType() {
        return AddonMenus.NETWORK_IRONS_ARCANE_ANVIL.get();
    }

    @Override
    public NetworkMenuSession starFantasySession() {
        return session;
    }

    @Override
    public StationKind starFantasyKind() {
        return StationKind.IRONS_ARCANE_ANVIL;
    }

    @Override
    public boolean stillValid(Player player) {
        return session == null || session.isUsable(player);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        boolean tookResult = slotId == StationKind.IRONS_ARCANE_ANVIL.resultSlot()
                && getSlot(slotId).hasItem();
        super.clicked(slotId, button, clickType, player);
        if (tookResult && session != null) {
            session.refillEmpty(this);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        boolean tookResult = index == StationKind.IRONS_ARCANE_ANVIL.resultSlot()
                && getSlot(index).hasItem();
        ItemStack moved = super.quickMoveStack(player, index);
        if (tookResult && !moved.isEmpty() && session != null) {
            session.refillEmpty(this);
        }
        return moved;
    }

    @Override
    public void removed(Player player) {
        if (session != null) {
            session.returnInputs(this);
        }
        super.removed(player);
        if (session != null) {
            session.returnToGridAfterClose();
        }
    }
}
