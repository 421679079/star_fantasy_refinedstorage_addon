package com.starfantasy.refinedstorageaddon.compat.quality.client;

import com.starfantasy.refinedstorageaddon.client.ClientGridReturn;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.qualityequipment.client.gui.ReforgingStationGUIScreen;
import net.qualityequipment.world.inventory.ReforgingStationGUIMenu;

public final class QualityEquipmentClientNetworkMenus {
    private QualityEquipmentClientNetworkMenus() {
    }

    public static AbstractContainerMenu createReplacement(AbstractContainerMenu oldMenu,
                                                           int containerId, Inventory inventory) {
        if (!(oldMenu instanceof ReforgingStationGUIMenu reforging)) {
            return null;
        }
        FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer());
        data.writeBlockPos(new BlockPos(reforging.x, reforging.y, reforging.z));
        return new NetworkReforgingStationClientMenu(containerId, inventory, data);
    }

    public static void openScreen(Minecraft minecraft, AbstractContainerMenu menu,
                                  Inventory inventory, Component title) {
        minecraft.setScreen(new NetworkReforgingStationScreen(
                (ReforgingStationGUIMenu) menu, inventory, title));
    }

    public static boolean isReplacement(AbstractContainerMenu menu) {
        return menu instanceof NetworkReforgingStationClientMenu;
    }

    public static final class NetworkReforgingStationClientMenu extends ReforgingStationGUIMenu {
        public NetworkReforgingStationClientMenu(int containerId, Inventory inventory,
                                                 FriendlyByteBuf data) {
            super(containerId, inventory, data);
        }
    }

    private static final class NetworkReforgingStationScreen extends ReforgingStationGUIScreen {
        private boolean returnRequested;

        private NetworkReforgingStationScreen(ReforgingStationGUIMenu menu, Inventory inventory,
                                               Component title) {
            super(menu, inventory, title);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == 256) {
                requestReturn();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public void onClose() {
            if (!requestReturn()) {
                super.onClose();
            }
        }

        private boolean requestReturn() {
            if (!returnRequested) {
                returnRequested = ClientGridReturn.request();
            }
            return returnRequested;
        }
    }
}
