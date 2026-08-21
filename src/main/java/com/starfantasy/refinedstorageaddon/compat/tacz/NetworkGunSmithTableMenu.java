package com.starfantasy.refinedstorageaddon.compat.tacz;

import com.starfantasy.refinedstorageaddon.network.AddonNetwork;
import com.starfantasy.refinedstorageaddon.network.ClientboundTaczNetworkConsumptionPacket;
import com.starfantasy.refinedstorageaddon.station.NetworkMenuSession;
import com.starfantasy.refinedstorageaddon.station.NetworkStationMenu;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageCraft;
import com.tacz.guns.resource.index.CommonBlockIndex;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;

public final class NetworkGunSmithTableMenu extends GunSmithTableMenu
        implements NetworkStationMenu {
    private final NetworkMenuSession session;
    private final StationKind kind;

    public NetworkGunSmithTableMenu(int containerId, Inventory inventory,
                                    ResourceLocation blockId, NetworkMenuSession session,
                                    StationKind kind) {
        super(containerId, inventory, blockId);
        this.session = session;
        this.kind = kind;
    }

    @Override
    public NetworkMenuSession starFantasySession() {
        return session;
    }

    @Override
    public StationKind starFantasyKind() {
        return kind;
    }

    @Override
    public boolean stillValid(Player player) {
        return session.isUsable(player);
    }

    @Override
    public void doCraft(ResourceLocation recipeId, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !session.isUsable(player)) {
            return;
        }
        GunSmithTableRecipe recipe = getAllowedRecipe(recipeId, serverPlayer);
        if (recipe == null) {
            return;
        }

        TaczCraftingMaterials.Result consumption = TaczCraftingMaterials.consume(
                session.network(), serverPlayer, recipe.getInputs());
        if (!consumption.success()) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.star_fantasy_refinedstorage_addon.missing_materials"), true);
            return;
        }

        ItemStack result = recipe.getResultItem(serverPlayer.level().registryAccess()).copy();
        if (!result.isEmpty()) {
            ItemEntity itemEntity = new ItemEntity(serverPlayer.level(), serverPlayer.getX(),
                    serverPlayer.getY() + 0.5, serverPlayer.getZ(), result);
            itemEntity.setPickUpDelay(0);
            serverPlayer.level().addFreshEntity(itemEntity);
        }
        serverPlayer.inventoryMenu.broadcastChanges();
        if (!consumption.networkConsumed().isEmpty()) {
            AddonNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new ClientboundTaczNetworkConsumptionPacket(
                            consumption.networkConsumed()));
        }
        NetworkHandler.sendToClientPlayer(new ServerMessageCraft(containerId), serverPlayer);
    }

    @Nullable
    private GunSmithTableRecipe getAllowedRecipe(ResourceLocation recipeId,
                                                  ServerPlayer player) {
        Recipe<?> rawRecipe = player.serverLevel().getRecipeManager()
                .byKey(recipeId).orElse(null);
        if (!(rawRecipe instanceof GunSmithTableRecipe recipe)) {
            return null;
        }

        ResourceLocation blockId = getBlockId();
        CommonBlockIndex block = TimelessAPI.getCommonBlockIndex(blockId).orElse(null);
        boolean unfilteredDefault = DefaultAssets.DEFAULT_BLOCK_ID.equals(blockId)
                && !SyncConfig.ENABLE_TABLE_FILTER.get();
        if (!unfilteredDefault && block != null && block.getFilter() != null
                && !block.getFilter().contains(recipeId)) {
            return null;
        }
        boolean missingTab = block == null || block.getData().getTabs().stream()
                .noneMatch(tab -> tab.id().equals(recipe.getTab()));
        return !unfilteredDefault && missingTab ? null : recipe;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        session.returnToGridAfterClose();
    }
}
