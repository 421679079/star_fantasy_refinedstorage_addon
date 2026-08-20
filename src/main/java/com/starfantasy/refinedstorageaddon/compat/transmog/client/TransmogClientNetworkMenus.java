package com.starfantasy.refinedstorageaddon.compat.transmog.client;

import com.hidoni.transmog.gui.TransmogScreen;
import com.hidoni.transmog.inventory.TransmogMenu;
import com.starfantasy.refinedstorageaddon.client.ClientGridReturn;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;

public final class TransmogClientNetworkMenus {
    private TransmogClientNetworkMenus() {
    }

    public static AbstractContainerMenu createReplacement(AbstractContainerMenu oldMenu,
                                                           int containerId, Inventory inventory) {
        return oldMenu instanceof TransmogMenu
                ? new NetworkTransmogClientMenu(containerId, inventory)
                : null;
    }

    public static void openScreen(Minecraft minecraft, AbstractContainerMenu menu,
                                  Inventory inventory, Component title) {
        minecraft.setScreen(new NetworkTransmogScreen((TransmogMenu) menu, inventory, title));
    }

    public static boolean isReplacement(AbstractContainerMenu menu) {
        return menu instanceof NetworkTransmogClientMenu;
    }

    public static final class NetworkTransmogClientMenu extends TransmogMenu {
        public NetworkTransmogClientMenu(int containerId, Inventory inventory) {
            super(containerId, inventory, ContainerLevelAccess.NULL, new ContainerData() {
                @Override
                public int get(int index) {
                    return index == 0 ? 3 : 0;
                }

                @Override
                public void set(int index, int value) {
                }

                @Override
                public int getCount() {
                    return 1;
                }
            });
        }
    }

    private static final class NetworkTransmogScreen extends TransmogScreen {
        private boolean returnRequested;

        private NetworkTransmogScreen(TransmogMenu menu, Inventory inventory, Component title) {
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
