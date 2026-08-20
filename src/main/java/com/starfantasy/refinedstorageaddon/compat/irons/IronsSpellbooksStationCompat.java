package com.starfantasy.refinedstorageaddon.compat.irons;

import com.starfantasy.refinedstorageaddon.station.NetworkMenuSession;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class IronsSpellbooksStationCompat {
    private IronsSpellbooksStationCompat() {
    }

    public static AbstractContainerMenu createInscriptionMenu(int containerId, Inventory inventory,
                                                               NetworkMenuSession session) {
        return new NetworkInscriptionTableMenu(containerId, inventory, session);
    }

    public static AbstractContainerMenu createArcaneAnvilMenu(int containerId, Inventory inventory,
                                                               NetworkMenuSession session) {
        return new NetworkArcaneAnvilMenu(containerId, inventory, session);
    }

    public static AbstractContainerMenu createScrollForgeMenu(int containerId, Inventory inventory,
                                                               NetworkMenuSession session) {
        return new NetworkScrollForgeMenu(containerId, inventory, session);
    }

    public static AbstractContainerMenu createClientInscriptionMenu(int containerId,
                                                                     Inventory inventory) {
        return new NetworkInscriptionTableMenu(containerId, inventory);
    }

    public static AbstractContainerMenu createClientArcaneAnvilMenu(int containerId,
                                                                    Inventory inventory) {
        return new NetworkArcaneAnvilMenu(containerId, inventory);
    }

    public static AbstractContainerMenu createClientScrollForgeMenu(int containerId,
                                                                    Inventory inventory) {
        return new NetworkScrollForgeMenu(containerId, inventory);
    }

    public static void prepareOnOpen(AbstractContainerMenu menu, NetworkMenuSession session,
                                     StationKind kind) {
        if (kind == StationKind.IRONS_SCROLL_FORGE) {
            session.fillEmptyInput(menu, 1, List.of(new ItemStack(Items.PAPER)), 0);
        }
    }

    public static void selectScrollSpell(ServerPlayer player, String spellId) {
        if (player.containerMenu instanceof NetworkScrollForgeMenu menu) {
            menu.selectSpell(player, spellId);
        }
    }

    public static void takeScrollResult(ServerPlayer player, String spellId, boolean quickMove) {
        if (player.containerMenu instanceof NetworkScrollForgeMenu menu) {
            menu.takeResult(player, spellId, quickMove);
        }
    }
}
