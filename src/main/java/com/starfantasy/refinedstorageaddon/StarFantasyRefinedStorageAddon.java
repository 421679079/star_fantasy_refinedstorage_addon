package com.starfantasy.refinedstorageaddon;

import com.starfantasy.refinedstorageaddon.network.AddonNetwork;
import com.starfantasy.refinedstorageaddon.registry.AddonMenus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(StarFantasyRefinedStorageAddon.MOD_ID)
public final class StarFantasyRefinedStorageAddon {
    public static final String MOD_ID = "star_fantasy_refinedstorage_addon";

    public StarFantasyRefinedStorageAddon() {
        AddonMenus.register(FMLJavaModLoadingContext.get().getModEventBus());
        AddonNetwork.register();
    }
}
