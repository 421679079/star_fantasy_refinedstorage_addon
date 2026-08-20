package com.starfantasy.refinedstorageaddon.client;

import com.starfantasy.refinedstorageaddon.StarFantasyRefinedStorageAddon;
import com.starfantasy.refinedstorageaddon.compat.disenchanting.client.NetworkDisenchantScreen;
import com.starfantasy.refinedstorageaddon.compat.irons.client.IronsSpellbooksClientMenus;
import com.starfantasy.refinedstorageaddon.registry.AddonMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = StarFantasyRefinedStorageAddon.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(AddonMenus.NETWORK_DISENCHANTER.get(),
                    NetworkDisenchantScreen::new);
            if (ModList.get().isLoaded("irons_spellbooks")) {
                IronsSpellbooksClientMenus.registerScreens();
            }
        });
    }
}
