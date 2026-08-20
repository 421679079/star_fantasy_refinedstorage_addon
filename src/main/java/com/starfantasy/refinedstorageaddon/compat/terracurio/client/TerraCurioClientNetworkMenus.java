package com.starfantasy.refinedstorageaddon.compat.terracurio.client;

import com.starfantasy.refinedstorageaddon.client.ClientGridReturn;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.confluence.terra_curio.client.renderer.gui.WorkshopScreen;
import org.confluence.terra_curio.menu.WorkshopMenu;

public final class TerraCurioClientNetworkMenus {
    private TerraCurioClientNetworkMenus() {
    }

    public static AbstractContainerMenu createReplacement(AbstractContainerMenu oldMenu,
                                                           int containerId, Inventory inventory) {
        return oldMenu instanceof WorkshopMenu
                ? new NetworkWorkshopClientMenu(containerId, inventory)
                : null;
    }

    public static void openScreen(Minecraft minecraft, AbstractContainerMenu menu,
                                  Inventory inventory, Component title) {
        minecraft.setScreen(new NetworkWorkshopScreen((WorkshopMenu) menu, inventory, title));
    }

    public static boolean isReplacement(AbstractContainerMenu menu) {
        return menu instanceof NetworkWorkshopClientMenu;
    }

    public static final class NetworkWorkshopClientMenu extends WorkshopMenu {
        public NetworkWorkshopClientMenu(int containerId, Inventory inventory) {
            super(containerId, inventory, ContainerLevelAccess.NULL);
        }
    }

    private static final class NetworkWorkshopScreen extends WorkshopScreen {
        private boolean returnRequested;

        private NetworkWorkshopScreen(WorkshopMenu menu, Inventory inventory, Component title) {
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
