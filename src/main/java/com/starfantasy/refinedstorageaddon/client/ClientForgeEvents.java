package com.starfantasy.refinedstorageaddon.client;

import com.refinedmods.refinedstorage.api.network.grid.GridType;
import com.refinedmods.refinedstorage.screen.grid.GridScreen;
import com.starfantasy.refinedstorageaddon.StarFantasyRefinedStorageAddon;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StarFantasyRefinedStorageAddon.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void onScreenInitialized(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof GridScreen gridScreen
                && gridScreen.getGrid().getGridType() == GridType.CRAFTING) {
            ClientStationState.install(gridScreen, event::addListener);
        }
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if ((event.getButton() == 0 || event.getButton() == 1)
                && ClientStationState.isOverAddonControl(
                event.getScreen(), event.getMouseX(), event.getMouseY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientStationState.clientTick();
        }
    }
}
