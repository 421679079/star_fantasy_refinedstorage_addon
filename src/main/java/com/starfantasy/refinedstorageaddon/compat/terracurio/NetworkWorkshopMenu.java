package com.starfantasy.refinedstorageaddon.compat.terracurio;

import com.starfantasy.refinedstorageaddon.station.NetworkMenuSession;
import com.starfantasy.refinedstorageaddon.station.NetworkStationMenu;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.confluence.terra_curio.menu.WorkshopMenu;

public final class NetworkWorkshopMenu extends WorkshopMenu implements NetworkStationMenu {
    private final NetworkMenuSession session;

    public NetworkWorkshopMenu(int containerId, Inventory inventory, NetworkMenuSession session) {
        super(containerId, inventory, session.levelAccess());
        this.session = session;
    }

    @Override
    public NetworkMenuSession starFantasySession() {
        return session;
    }

    @Override
    public StationKind starFantasyKind() {
        return StationKind.TERRA_WORKSHOP;
    }

    @Override
    public boolean stillValid(Player player) {
        return session.isUsable(player);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        boolean tookResult = slotId == StationKind.TERRA_WORKSHOP.resultSlot()
                && getSlot(slotId).hasItem();
        super.clicked(slotId, button, clickType, player);
        if (tookResult) {
            session.refillEmpty(this);
        }
    }

    public void selectRecipe(Player player, ItemStack expected) {
        for (int index = 0; index < getRecipesAmount(); index++) {
            clickMenuButton(player, index);
            ItemStack result = getSlot(StationKind.TERRA_WORKSHOP.resultSlot()).getItem();
            if (ItemStack.isSameItemSameTags(result, expected)
                    && result.getCount() == expected.getCount()) {
                broadcastChanges();
                return;
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
