package com.starfantasy.refinedstorageaddon.compat.transmog;

import com.hidoni.transmog.inventory.TransmogMenu;
import com.starfantasy.refinedstorageaddon.station.NetworkMenuSession;
import com.starfantasy.refinedstorageaddon.station.NetworkStationMenu;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class NetworkTransmogMenu extends TransmogMenu implements NetworkStationMenu {
    private static final int PLAYER_START = 4;
    private static final int PLAYER_END = 40;
    private final NetworkMenuSession session;

    public NetworkTransmogMenu(int containerId, Inventory inventory, NetworkMenuSession session) {
        super(containerId, inventory, session.levelAccess(), TransmogStationCompat.infiniteFuel());
        this.session = session;
    }

    @Override
    public NetworkMenuSession starFantasySession() {
        return session;
    }

    @Override
    public StationKind starFantasyKind() {
        return StationKind.TRANSMOGRIFICATION_TABLE;
    }

    @Override
    public boolean stillValid(Player player) {
        return session.isUsable(player);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == FUEL_ITEM_SLOT) {
            return;
        }
        boolean tookResult = slotId == OUTPUT_SLOT && getSlot(slotId).hasItem();
        super.clicked(slotId, button, clickType, player);
        if (tookResult) {
            session.refillEmpty(this);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = getSlot(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
            if (index == OUTPUT_SLOT) {
                slot.onQuickCraft(stack, original);
            }
        } else if (!moveItemStackTo(stack, ITEM_TO_TRANSMOG_SLOT, FUEL_ITEM_SLOT, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        if (index == OUTPUT_SLOT) {
            session.refillEmpty(this);
        }
        return original;
    }

    @Override
    public void removed(Player player) {
        session.returnInputs(this);
        super.removed(player);
        session.returnToGridAfterClose();
    }
}
