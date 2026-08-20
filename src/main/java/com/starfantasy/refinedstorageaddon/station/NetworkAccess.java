package com.starfantasy.refinedstorageaddon.station;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.network.grid.GridType;
import com.refinedmods.refinedstorage.api.network.grid.IGrid;
import com.refinedmods.refinedstorage.api.network.grid.INetworkAwareGrid;
import com.refinedmods.refinedstorage.api.network.security.Permission;
import com.refinedmods.refinedstorage.container.GridContainerMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nullable;

public final class NetworkAccess {
    private NetworkAccess() {
    }

    @Nullable
    public static INetwork fromMenu(AbstractContainerMenu menu) {
        if (menu instanceof NetworkStationMenu stationMenu) {
            return stationMenu.starFantasySession().network();
        }
        if (!(menu instanceof GridContainerMenu gridMenu)) {
            return null;
        }
        IGrid grid = gridMenu.getGrid();
        if (grid.getGridType() != GridType.CRAFTING) {
            return null;
        }
        return grid instanceof INetworkAwareGrid networkGrid ? networkGrid.getNetwork() : null;
    }

    public static boolean canUse(@Nullable INetwork network, ServerPlayer player) {
        return network != null && network.canRun()
                && network.getSecurityManager().hasPermission(Permission.MODIFY, player)
                && network.getSecurityManager().hasPermission(Permission.EXTRACT, player);
    }
}
