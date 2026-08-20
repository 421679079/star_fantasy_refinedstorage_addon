package com.starfantasy.refinedstorageaddon.station;

import com.refinedmods.refinedstorage.container.GridContainerMenu;
import com.starfantasy.refinedstorageaddon.network.AddonNetwork;
import com.starfantasy.refinedstorageaddon.network.ClientboundStationSlotsPacket;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

public final class StationSlotService {
    private StationSlotService() {
    }

    public static void sync(ServerPlayer player, int expectedContainerId) {
        GridContainerMenu menu = currentMenu(player, expectedContainerId);
        if (menu != null) {
            send(player, menu);
        }
    }

    public static void click(ServerPlayer player, int expectedContainerId, int slotIndex,
                             int mouseButton) {
        GridContainerMenu menu = currentMenu(player, expectedContainerId);
        if (menu == null || slotIndex < 0 || slotIndex >= StationSlotStorage.SLOT_COUNT
                || (mouseButton != 0 && mouseButton != 1)) {
            return;
        }
        if (!NetworkAccess.canUse(NetworkAccess.fromMenu(menu), player)) {
            return;
        }

        NonNullList<ItemStack> slots = StationSlotStorage.read(menu);
        ItemStack stored = slots.get(slotIndex);
        ItemStack carried = menu.getCarried();
        boolean changed = false;

        if (carried.isEmpty()) {
            if (!stored.isEmpty()) {
                menu.setCarried(stored.copy());
                slots.set(slotIndex, ItemStack.EMPTY);
                changed = true;
            }
        } else {
            StationKind carriedKind = StationKind.fromActivationItem(carried);
            if (carriedKind == null) {
                player.displayClientMessage(Component.translatable(
                        "message.star_fantasy_refinedstorage_addon.invalid_station_item"), true);
                return;
            }
            boolean duplicate = false;
            for (int index = 0; index < slots.size(); index++) {
                if (index != slotIndex && carriedKind.isActivationItem(slots.get(index))) {
                    duplicate = true;
                    break;
                }
            }
            if (duplicate) {
                player.displayClientMessage(Component.translatable(
                        "message.star_fantasy_refinedstorage_addon.duplicate_station"), true);
                return;
            }

            ItemStack inserted = carried.copy();
            inserted.setCount(1);
            if (stored.isEmpty()) {
                ItemStack remainder = carried.copy();
                remainder.shrink(1);
                menu.setCarried(remainder.isEmpty() ? ItemStack.EMPTY : remainder);
                slots.set(slotIndex, inserted);
                changed = true;
            } else if (carried.getCount() == 1) {
                menu.setCarried(stored.copy());
                slots.set(slotIndex, inserted);
                changed = true;
            }
        }

        if (changed) {
            StationSlotStorage.write(menu, slots, player);
            menu.broadcastChanges();
            broadcast(player, menu);
        }
    }

    private static void broadcast(ServerPlayer source, GridContainerMenu sourceMenu) {
        send(source, sourceMenu);
        if (sourceMenu.getBlockEntity() == null) {
            return;
        }
        for (ServerPlayer target : source.server.getPlayerList().getPlayers()) {
            if (target == source || !(target.containerMenu instanceof GridContainerMenu targetMenu)
                    || !StationSlotStorage.sameBlockBacking(sourceMenu, targetMenu)) {
                continue;
            }
            send(target, targetMenu);
        }
    }

    private static void send(ServerPlayer player, GridContainerMenu menu) {
        AddonNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ClientboundStationSlotsPacket(menu.containerId,
                        StationSlotStorage.snapshot(menu)));
    }

    private static GridContainerMenu currentMenu(ServerPlayer player, int expectedContainerId) {
        AbstractContainerMenu current = player.containerMenu;
        if (current.containerId != expectedContainerId
                || !(current instanceof GridContainerMenu gridMenu)
                || !StationSlotStorage.isSupported(gridMenu)) {
            return null;
        }
        return gridMenu;
    }
}
