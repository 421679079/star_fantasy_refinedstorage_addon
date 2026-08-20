package com.starfantasy.refinedstorageaddon.compat.goety.client;

import com.Polarice3.Goety.client.gui.screen.inventory.DarkAnvilScreen;
import com.Polarice3.Goety.client.inventory.container.DarkAnvilMenu;
import com.starfantasy.refinedstorageaddon.client.ClientGridReturn;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;

public final class GoetyClientNetworkMenus {
    private GoetyClientNetworkMenus() {
    }

    public static AbstractContainerMenu createReplacement(AbstractContainerMenu oldMenu,
                                                           int containerId, Inventory inventory) {
        return oldMenu instanceof DarkAnvilMenu
                ? new NetworkDarkAnvilClientMenu(containerId, inventory)
                : null;
    }

    public static void openScreen(Minecraft minecraft, AbstractContainerMenu menu,
                                  Inventory inventory, Component title) {
        minecraft.setScreen(new NetworkDarkAnvilScreen((DarkAnvilMenu) menu, inventory, title));
    }

    public static boolean isReplacement(AbstractContainerMenu menu) {
        return menu instanceof NetworkDarkAnvilClientMenu;
    }

    public static final class NetworkDarkAnvilClientMenu extends DarkAnvilMenu {
        public NetworkDarkAnvilClientMenu(int containerId, Inventory inventory) {
            super(containerId, inventory, ContainerLevelAccess.NULL);
        }
    }

    private static final class NetworkDarkAnvilScreen extends DarkAnvilScreen {
        private boolean returnRequested;

        private NetworkDarkAnvilScreen(DarkAnvilMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
        }

        @Override
        public void onClose() {
            if (!returnRequested) {
                returnRequested = ClientGridReturn.request();
            }
            if (!returnRequested) {
                super.onClose();
            }
        }
    }
}
