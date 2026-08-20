package com.starfantasy.refinedstorageaddon.compat.irons.client;

import com.starfantasy.refinedstorageaddon.client.ClientGridReturn;
import com.starfantasy.refinedstorageaddon.network.AddonNetwork;
import com.starfantasy.refinedstorageaddon.network.ServerboundSelectIronsScrollSpellPacket;
import com.starfantasy.refinedstorageaddon.network.ServerboundTakeIronsScrollResultPacket;
import com.starfantasy.refinedstorageaddon.registry.AddonMenus;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilScreen;
import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableScreen;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeScreen;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public final class IronsSpellbooksClientMenus {
    private IronsSpellbooksClientMenus() {
    }

    @SuppressWarnings("unchecked")
    public static void registerScreens() {
        MenuScreens.register(
                (MenuType<InscriptionTableMenu>) (MenuType<?>)
                        AddonMenus.NETWORK_IRONS_INSCRIPTION_TABLE.get(),
                NetworkInscriptionScreen::new);
        MenuScreens.register(
                (MenuType<ArcaneAnvilMenu>) (MenuType<?>)
                        AddonMenus.NETWORK_IRONS_ARCANE_ANVIL.get(),
                NetworkArcaneAnvilScreen::new);
        MenuScreens.register(
                (MenuType<ScrollForgeMenu>) (MenuType<?>)
                        AddonMenus.NETWORK_IRONS_SCROLL_FORGE.get(),
                NetworkScrollForgeScreen::new);
    }

    private static final class NetworkInscriptionScreen extends InscriptionTableScreen {
        private boolean returnRequested;

        private NetworkInscriptionScreen(InscriptionTableMenu menu, Inventory inventory,
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

    private static final class NetworkArcaneAnvilScreen extends ArcaneAnvilScreen {
        private boolean returnRequested;

        private NetworkArcaneAnvilScreen(ArcaneAnvilMenu menu, Inventory inventory,
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

    private static final class NetworkScrollForgeScreen extends ScrollForgeScreen {
        private boolean returnRequested;
        private String lastSyncedSpell;

        private NetworkScrollForgeScreen(ScrollForgeMenu menu, Inventory inventory,
                                         Component title) {
            super(menu, inventory, title);
            // The server may already have selected a JEI recipe before this screen is created.
            // Treat the screen's initial "none" value as synchronized so it cannot erase that
            // server-side selection on the first render.
            this.lastSyncedSpell = getSelectedSpell().getSpellId();
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.render(graphics, mouseX, mouseY, partialTick);
            syncSelectedSpell();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if ((button == 0 || button == 1) && hoveredSlot != null
                    && hoveredSlot.index == StationKind.IRONS_SCROLL_FORGE.resultSlot()
                    && hoveredSlot.hasItem()) {
                SpellData spell = ISpellContainer.get(hoveredSlot.getItem()).getSpellAtIndex(0);
                if (spell != SpellData.EMPTY) {
                    AddonNetwork.CHANNEL.sendToServer(new ServerboundTakeIronsScrollResultPacket(
                            spell.getSpell().getSpellId(), hasShiftDown()));
                    return true;
                }
            }
            boolean handled = super.mouseClicked(mouseX, mouseY, button);
            syncSelectedSpell();
            return handled;
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

        private void syncSelectedSpell() {
            String spellId = getSelectedSpell().getSpellId();
            if (!spellId.equals(lastSyncedSpell)) {
                lastSyncedSpell = spellId;
                AddonNetwork.CHANNEL.sendToServer(
                        new ServerboundSelectIronsScrollSpellPacket(spellId));
            }
        }
    }
}
