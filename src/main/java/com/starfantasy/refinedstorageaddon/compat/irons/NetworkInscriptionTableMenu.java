package com.starfantasy.refinedstorageaddon.compat.irons;

import com.starfantasy.refinedstorageaddon.registry.AddonMenus;
import com.starfantasy.refinedstorageaddon.station.NetworkMenuSession;
import com.starfantasy.refinedstorageaddon.station.NetworkStationMenu;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class NetworkInscriptionTableMenu extends InscriptionTableMenu
        implements NetworkStationMenu {
    @Nullable
    private final NetworkMenuSession session;
    private final boolean usesCurioSpellbook;

    public NetworkInscriptionTableMenu(int containerId, Inventory inventory) {
        super(containerId, inventory, ContainerLevelAccess.NULL);
        this.session = null;
        this.usesCurioSpellbook = getSpellBookSlot().hasItem();
    }

    public NetworkInscriptionTableMenu(int containerId, Inventory inventory,
                                       NetworkMenuSession session) {
        super(containerId, inventory, session.levelAccess());
        this.session = session;
        this.usesCurioSpellbook = getSpellBookSlot().hasItem();
    }

    @Override
    public MenuType<?> getType() {
        return AddonMenus.NETWORK_IRONS_INSCRIPTION_TABLE.get();
    }

    @Override
    public NetworkMenuSession starFantasySession() {
        return session;
    }

    @Override
    public StationKind starFantasyKind() {
        return StationKind.IRONS_INSCRIPTION_TABLE;
    }

    @Override
    public boolean stillValid(Player player) {
        return session == null || session.isUsable(player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (session != null && id < 0) {
            session.rememberInput(this, 1, 1);
        }
        int before = getScrollSlot().getItem().getCount();
        boolean handled = super.clickMenuButton(player, id);
        if (handled && id < 0 && session != null
                && getScrollSlot().getItem().getCount() < before) {
            session.refillEmpty(this);
        }
        return handled;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = super.quickMoveStack(player, index);
        if (session != null && !moved.isEmpty()) {
            session.rememberInput(this, 0, 1);
            session.rememberInput(this, 1, 1);
        }
        return moved;
    }

    @Override
    public void removed(Player player) {
        if (session != null) {
            if (usesCurioSpellbook) {
                session.returnInputsExcept(this, 0);
            } else {
                session.returnInputs(this);
            }
        }
        super.removed(player);
        if (session != null) {
            session.returnToGridAfterClose();
        }
    }
}
