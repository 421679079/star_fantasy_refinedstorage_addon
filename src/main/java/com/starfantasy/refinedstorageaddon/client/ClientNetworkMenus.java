package com.starfantasy.refinedstorageaddon.client;

import com.starfantasy.refinedstorageaddon.compat.goety.client.GoetyClientNetworkMenus;
import com.starfantasy.refinedstorageaddon.compat.disenchanting.NetworkDisenchantMenu;
import com.starfantasy.refinedstorageaddon.compat.irons.NetworkArcaneAnvilMenu;
import com.starfantasy.refinedstorageaddon.compat.irons.NetworkInscriptionTableMenu;
import com.starfantasy.refinedstorageaddon.compat.irons.NetworkScrollForgeMenu;
import com.starfantasy.refinedstorageaddon.compat.quality.client.QualityEquipmentClientNetworkMenus;
import com.starfantasy.refinedstorageaddon.compat.terracurio.client.TerraCurioClientNetworkMenus;
import com.starfantasy.refinedstorageaddon.compat.transmog.client.TransmogClientNetworkMenus;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;

/** Gives JEI a distinct client menu class without changing vanilla workstation menu types. */
public final class ClientNetworkMenus {
    private ClientNetworkMenus() {
    }

    public static void replaceActive(int containerId, StationKind kind) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.player.containerMenu.containerId != containerId
                || !(minecraft.screen instanceof AbstractContainerScreen<?> oldScreen)) {
            return;
        }
        AbstractContainerMenu oldMenu = minecraft.player.containerMenu;
        if (isReplacement(oldMenu)) {
            return;
        }

        Inventory inventory = minecraft.player.getInventory();
        AbstractContainerMenu newMenu = switch (kind) {
            case STONECUTTER -> oldMenu instanceof StonecutterMenu
                    ? new NetworkStonecutterClientMenu(containerId, inventory) : null;
            case SMITHING -> oldMenu instanceof SmithingMenu
                    ? new NetworkSmithingClientMenu(containerId, inventory) : null;
            case ANVIL -> oldMenu instanceof AnvilMenu
                    ? new NetworkAnvilClientMenu(containerId, inventory) : null;
            case GRINDSTONE -> oldMenu instanceof GrindstoneMenu
                    ? new NetworkGrindstoneClientMenu(containerId, inventory) : null;
            case GOETY_DARK_ANVIL -> GoetyClientNetworkMenus.createReplacement(
                    oldMenu, containerId, inventory);
            case TRANSMOGRIFICATION_TABLE -> TransmogClientNetworkMenus.createReplacement(
                    oldMenu, containerId, inventory);
            case QUALITY_REFORGING_STATION -> QualityEquipmentClientNetworkMenus.createReplacement(
                    oldMenu, containerId, inventory);
            case TERRA_WORKSHOP -> TerraCurioClientNetworkMenus.createReplacement(
                    oldMenu, containerId, inventory);
            case DISENCHANTER -> null;
            case IRONS_INSCRIPTION_TABLE, IRONS_ARCANE_ANVIL, IRONS_SCROLL_FORGE -> null;
        };
        if (newMenu == null) {
            return;
        }

        newMenu.initializeContents(oldMenu.getStateId(), oldMenu.getItems(), oldMenu.getCarried());
        Component title = oldScreen.getTitle();
        minecraft.player.containerMenu = newMenu;
        switch (kind) {
            case STONECUTTER -> minecraft.setScreen(new NetworkStonecutterScreen(
                    (StonecutterMenu) newMenu, inventory, title));
            case SMITHING -> minecraft.setScreen(new NetworkSmithingScreen(
                    (SmithingMenu) newMenu, inventory, title));
            case ANVIL -> minecraft.setScreen(new NetworkAnvilScreen(
                    (AnvilMenu) newMenu, inventory, title));
            case GRINDSTONE -> minecraft.setScreen(new NetworkGrindstoneScreen(
                    (GrindstoneMenu) newMenu, inventory, title));
            case GOETY_DARK_ANVIL -> GoetyClientNetworkMenus.openScreen(
                    minecraft, newMenu, inventory, title);
            case TRANSMOGRIFICATION_TABLE -> TransmogClientNetworkMenus.openScreen(
                    minecraft, newMenu, inventory, title);
            case QUALITY_REFORGING_STATION -> QualityEquipmentClientNetworkMenus.openScreen(
                    minecraft, newMenu, inventory, title);
            case TERRA_WORKSHOP -> TerraCurioClientNetworkMenus.openScreen(
                    minecraft, newMenu, inventory, title);
            case DISENCHANTER -> {
            }
            case IRONS_INSCRIPTION_TABLE, IRONS_ARCANE_ANVIL, IRONS_SCROLL_FORGE -> {
            }
        }
    }

    private static boolean isReplacement(AbstractContainerMenu menu) {
        return menu instanceof NetworkStonecutterClientMenu
                || menu instanceof NetworkSmithingClientMenu
                || menu instanceof NetworkAnvilClientMenu
                || menu instanceof NetworkGrindstoneClientMenu
                || (StationKind.GOETY_DARK_ANVIL.isInstalled()
                && GoetyClientNetworkMenus.isReplacement(menu))
                || (StationKind.TRANSMOGRIFICATION_TABLE.isInstalled()
                && TransmogClientNetworkMenus.isReplacement(menu))
                || (StationKind.QUALITY_REFORGING_STATION.isInstalled()
                && QualityEquipmentClientNetworkMenus.isReplacement(menu))
                || (StationKind.TERRA_WORKSHOP.isInstalled()
                && TerraCurioClientNetworkMenus.isReplacement(menu))
                || menu instanceof NetworkDisenchantMenu
                || (StationKind.IRONS_INSCRIPTION_TABLE.isInstalled()
                && (menu instanceof NetworkInscriptionTableMenu
                || menu instanceof NetworkArcaneAnvilMenu
                || menu instanceof NetworkScrollForgeMenu));
    }

    public static final class NetworkStonecutterClientMenu extends StonecutterMenu {
        public NetworkStonecutterClientMenu(int containerId, Inventory inventory) {
            super(containerId, inventory, ContainerLevelAccess.NULL);
        }
    }

    public static final class NetworkSmithingClientMenu extends SmithingMenu {
        public NetworkSmithingClientMenu(int containerId, Inventory inventory) {
            super(containerId, inventory, ContainerLevelAccess.NULL);
        }
    }

    public static final class NetworkAnvilClientMenu extends AnvilMenu {
        public NetworkAnvilClientMenu(int containerId, Inventory inventory) {
            super(containerId, inventory, ContainerLevelAccess.NULL);
        }
    }

    public static final class NetworkGrindstoneClientMenu extends GrindstoneMenu {
        public NetworkGrindstoneClientMenu(int containerId, Inventory inventory) {
            super(containerId, inventory, ContainerLevelAccess.NULL);
        }
    }

    private static final class ReturnRequest {
        private boolean returnRequested;

        protected final boolean requestReturn() {
            if (returnRequested) {
                return true;
            }
            returnRequested = ClientGridReturn.request();
            return returnRequested;
        }
    }

    private static final class NetworkStonecutterScreen extends StonecutterScreen {
        private final ReturnRequest returning = new ReturnRequest();

        private NetworkStonecutterScreen(StonecutterMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
        }

        @Override
        public void onClose() {
            if (!returning.requestReturn()) {
                super.onClose();
            }
        }
    }

    private static final class NetworkSmithingScreen extends SmithingScreen {
        private final ReturnRequest returning = new ReturnRequest();

        private NetworkSmithingScreen(SmithingMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
        }

        @Override
        public void onClose() {
            if (!returning.requestReturn()) {
                super.onClose();
            }
        }
    }

    private static final class NetworkAnvilScreen extends AnvilScreen {
        private final ReturnRequest returning = new ReturnRequest();

        private NetworkAnvilScreen(AnvilMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
        }

        @Override
        public void onClose() {
            if (!returning.requestReturn()) {
                super.onClose();
            }
        }
    }

    private static final class NetworkGrindstoneScreen extends GrindstoneScreen {
        private final ReturnRequest returning = new ReturnRequest();

        private NetworkGrindstoneScreen(GrindstoneMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
        }

        @Override
        public void onClose() {
            if (!returning.requestReturn()) {
                super.onClose();
            }
        }
    }
}
