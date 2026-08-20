package com.starfantasy.refinedstorageaddon.station;

import com.refinedmods.refinedstorage.api.network.grid.GridType;
import com.refinedmods.refinedstorage.api.network.grid.IGrid;
import com.refinedmods.refinedstorage.blockentity.BaseBlockEntity;
import com.refinedmods.refinedstorage.blockentity.grid.WirelessGrid;
import com.refinedmods.refinedstorage.blockentity.grid.portable.PortableGrid;
import com.refinedmods.refinedstorage.container.GridContainerMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/** Seven small, persistent workstation module slots owned by a crafting grid. */
public final class StationSlotStorage {
    public static final int SLOT_COUNT = 7;
    private static final String CONTENTS_TAG = "StarFantasyWorkstationSlots";
    private static final String SLOT_TAG = "Slot";

    private StationSlotStorage() {
    }

    public static boolean isSupported(GridContainerMenu menu) {
        IGrid grid = menu.getGrid();
        return grid.getGridType() == GridType.CRAFTING
                && (isItemBacked(grid) || menu.getBlockEntity() != null);
    }

    public static NonNullList<ItemStack> read(GridContainerMenu menu) {
        CompoundTag root = backingTag(menu, false);
        return root == null ? emptySlots() : read(root);
    }

    public static List<ItemStack> snapshot(GridContainerMenu menu) {
        return read(menu).stream().map(ItemStack::copy).toList();
    }

    public static boolean hasKind(GridContainerMenu menu, StationKind kind) {
        return read(menu).stream().anyMatch(kind::isActivationItem);
    }

    public static void write(GridContainerMenu menu, NonNullList<ItemStack> slots,
                             ServerPlayer player) {
        CompoundTag root = backingTag(menu, true);
        if (root == null) {
            return;
        }
        write(root, slots);
        BaseBlockEntity blockEntity = menu.getBlockEntity();
        if (!isItemBacked(menu.getGrid()) && blockEntity != null) {
            blockEntity.setChanged();
        } else {
            player.getInventory().setChanged();
        }
    }

    public static List<ItemStack> takeFromBlockEntity(BaseBlockEntity blockEntity) {
        CompoundTag root = blockEntity.getPersistentData();
        if (!root.contains(CONTENTS_TAG, Tag.TAG_LIST)) {
            return List.of();
        }
        List<ItemStack> result = read(root).stream()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
        root.remove(CONTENTS_TAG);
        blockEntity.setChanged();
        return result;
    }

    public static boolean sameBlockBacking(GridContainerMenu first, GridContainerMenu second) {
        if (isItemBacked(first.getGrid()) || isItemBacked(second.getGrid())) {
            return false;
        }
        BaseBlockEntity firstBlock = first.getBlockEntity();
        BaseBlockEntity secondBlock = second.getBlockEntity();
        return firstBlock != null && secondBlock != null
                && firstBlock.getLevel() == secondBlock.getLevel()
                && firstBlock.getBlockPos().equals(secondBlock.getBlockPos());
    }

    private static NonNullList<ItemStack> read(CompoundTag root) {
        NonNullList<ItemStack> slots = emptySlots();
        ListTag contents = root.getList(CONTENTS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < contents.size(); index++) {
            CompoundTag itemTag = contents.getCompound(index);
            int slot = itemTag.getByte(SLOT_TAG) & 255;
            if (slot < SLOT_COUNT) {
                ItemStack stack = ItemStack.of(itemTag);
                if (StationKind.fromActivationItem(stack) != null) {
                    stack.setCount(1);
                    slots.set(slot, stack);
                }
            }
        }
        return slots;
    }

    private static void write(CompoundTag root, NonNullList<ItemStack> slots) {
        ListTag contents = new ListTag();
        for (int slot = 0; slot < Math.min(slots.size(), SLOT_COUNT); slot++) {
            ItemStack stack = slots.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag itemTag = new CompoundTag();
            itemTag.putByte(SLOT_TAG, (byte) slot);
            ItemStack single = stack.copy();
            single.setCount(1);
            single.save(itemTag);
            contents.add(itemTag);
        }
        if (contents.isEmpty()) {
            root.remove(CONTENTS_TAG);
        } else {
            root.put(CONTENTS_TAG, contents);
        }
    }

    private static NonNullList<ItemStack> emptySlots() {
        return NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    }

    @Nullable
    private static CompoundTag backingTag(GridContainerMenu menu, boolean create) {
        IGrid grid = menu.getGrid();
        if (grid instanceof WirelessGrid wirelessGrid) {
            return itemTag(wirelessGrid.getStack(), create);
        }
        if (grid instanceof PortableGrid portableGrid) {
            return itemTag(portableGrid.getStack(), create);
        }
        BaseBlockEntity blockEntity = menu.getBlockEntity();
        return blockEntity == null ? null : blockEntity.getPersistentData();
    }

    @Nullable
    private static CompoundTag itemTag(ItemStack stack, boolean create) {
        return create ? stack.getOrCreateTag() : stack.getTag();
    }

    private static boolean isItemBacked(IGrid grid) {
        return grid instanceof WirelessGrid || grid instanceof PortableGrid;
    }
}
