package com.starfantasy.refinedstorageaddon.compat.tacz.client;

import com.starfantasy.refinedstorageaddon.client.ClientGridReturn;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class TaczClientNetworkMenus {
    private TaczClientNetworkMenus() {
    }

    public static AbstractContainerMenu createReplacement(AbstractContainerMenu oldMenu,
                                                          int containerId,
                                                          Inventory inventory) {
        if (!(oldMenu instanceof GunSmithTableMenu gunSmithMenu)
                || gunSmithMenu.getBlockId() == null) {
            return null;
        }
        return new NetworkGunSmithTableClientMenu(containerId, inventory,
                gunSmithMenu.getBlockId());
    }

    public static boolean isReplacement(AbstractContainerMenu menu) {
        return menu instanceof NetworkGunSmithTableClientMenu;
    }

    public static void openScreen(Minecraft minecraft, AbstractContainerMenu menu,
                                  Inventory inventory, Component title) {
        minecraft.setScreen(new NetworkGunSmithTableScreen(
                (GunSmithTableMenu) menu, inventory, title));
    }

    private static final class NetworkGunSmithTableClientMenu extends GunSmithTableMenu {
        private NetworkGunSmithTableClientMenu(int containerId, Inventory inventory,
                                               ResourceLocation blockId) {
            super(containerId, inventory, blockId);
        }
    }

    private static final class NetworkGunSmithTableScreen extends GunSmithTableScreen {
        private boolean returnRequested;

        private NetworkGunSmithTableScreen(GunSmithTableMenu menu, Inventory inventory,
                                           Component title) {
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
