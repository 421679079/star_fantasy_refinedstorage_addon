package com.starfantasy.refinedstorageaddon.station;

import com.refinedmods.refinedstorage.api.network.grid.IGrid;
import com.refinedmods.refinedstorage.apiimpl.API;
import com.refinedmods.refinedstorage.apiimpl.network.grid.factory.GridBlockGridFactory;
import com.refinedmods.refinedstorage.apiimpl.network.grid.factory.PortableGridBlockGridFactory;
import com.refinedmods.refinedstorage.apiimpl.network.grid.factory.WirelessGridGridFactory;
import com.refinedmods.refinedstorage.blockentity.BaseBlockEntity;
import com.refinedmods.refinedstorage.blockentity.grid.WirelessGrid;
import com.refinedmods.refinedstorage.blockentity.grid.portable.IPortableGrid;
import com.refinedmods.refinedstorage.container.GridContainerMenu;
import com.refinedmods.refinedstorage.inventory.player.PlayerSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** Remembers how the player opened RS so a network workstation can return to it safely. */
public final class GridOrigin {
    private final ResourceLocation factoryId;
    @Nullable
    private final BlockPos blockPos;
    private final int playerSlot;

    private GridOrigin(ResourceLocation factoryId, @Nullable BlockPos blockPos, int playerSlot) {
        this.factoryId = factoryId;
        this.blockPos = blockPos;
        this.playerSlot = playerSlot;
    }

    @Nullable
    public static GridOrigin capture(ServerPlayer player, AbstractContainerMenu menu) {
        if (menu instanceof NetworkStationMenu stationMenu) {
            return stationMenu.starFantasySession().origin();
        }
        if (!(menu instanceof GridContainerMenu gridMenu)) {
            return null;
        }

        BaseBlockEntity blockEntity = gridMenu.getBlockEntity();
        IGrid grid = gridMenu.getGrid();
        if (blockEntity != null) {
            ResourceLocation factory = grid instanceof IPortableGrid
                    ? PortableGridBlockGridFactory.ID
                    : GridBlockGridFactory.ID;
            return new GridOrigin(factory, blockEntity.getBlockPos().immutable(), -1);
        }
        if (grid instanceof WirelessGrid wirelessGrid) {
            int slot = findPlayerSlot(player.getInventory(), wirelessGrid.getStack());
            if (slot >= 0) {
                return new GridOrigin(WirelessGridGridFactory.ID, null, slot);
            }
        }
        return null;
    }

    public boolean reopen(ServerPlayer player) {
        AbstractContainerMenu previous = player.containerMenu;
        if (blockPos != null) {
            API.instance().getGridManager().openGrid(factoryId, player, blockPos);
            return player.containerMenu != previous && player.containerMenu instanceof GridContainerMenu;
        }
        if (playerSlot < 0 || playerSlot >= player.getInventory().getContainerSize()) {
            return false;
        }
        ItemStack stack = player.getInventory().getItem(playerSlot);
        if (!stack.isEmpty()) {
            API.instance().getGridManager().openGrid(factoryId, player, stack, new PlayerSlot(playerSlot));
        }
        return player.containerMenu != previous && player.containerMenu instanceof GridContainerMenu;
    }

    private static int findPlayerSlot(Inventory inventory, ItemStack expected) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot) == expected) {
                return slot;
            }
        }
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (ItemStack.isSameItemSameTags(inventory.getItem(slot), expected)) {
                return slot;
            }
        }
        return -1;
    }
}
