package com.starfantasy.refinedstorageaddon.registry;

import com.starfantasy.refinedstorageaddon.StarFantasyRefinedStorageAddon;
import com.starfantasy.refinedstorageaddon.compat.disenchanting.NetworkDisenchantMenu;
import com.starfantasy.refinedstorageaddon.compat.irons.IronsSpellbooksStationCompat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class AddonMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(
            ForgeRegistries.MENU_TYPES, StarFantasyRefinedStorageAddon.MOD_ID);

    public static final RegistryObject<MenuType<NetworkDisenchantMenu>> NETWORK_DISENCHANTER =
            MENUS.register("network_disenchanter",
                    () -> new MenuType<>(NetworkDisenchantMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<MenuType<AbstractContainerMenu>> NETWORK_IRONS_INSCRIPTION_TABLE =
            MENUS.register("network_irons_inscription_table",
                    () -> new MenuType<>((containerId, inventory) ->
                            IronsSpellbooksStationCompat.createClientInscriptionMenu(
                                    containerId, inventory),
                            FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<MenuType<AbstractContainerMenu>> NETWORK_IRONS_ARCANE_ANVIL =
            MENUS.register("network_irons_arcane_anvil",
                    () -> new MenuType<>((containerId, inventory) ->
                            IronsSpellbooksStationCompat.createClientArcaneAnvilMenu(
                                    containerId, inventory),
                            FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<MenuType<AbstractContainerMenu>> NETWORK_IRONS_SCROLL_FORGE =
            MENUS.register("network_irons_scroll_forge",
                    () -> new MenuType<>((containerId, inventory) ->
                            IronsSpellbooksStationCompat.createClientScrollForgeMenu(
                                    containerId, inventory),
                            FeatureFlags.DEFAULT_FLAGS));

    private AddonMenus() {
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
