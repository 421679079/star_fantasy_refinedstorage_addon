package com.starfantasy.refinedstorageaddon.station;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.network.security.Permission;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.api.util.IComparer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.TickTask;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

public final class NetworkMenuSession {
    private final INetwork network;
    private final ServerPlayer owner;
    private final StationKind kind;
    @Nullable
    private final GridOrigin origin;
    private final List<ItemStack> refillPatterns;
    private final List<Integer> refillMinimumCounts;
    private boolean movingItems;
    private boolean suppressReturnToGrid;

    public NetworkMenuSession(INetwork network, ServerPlayer owner, StationKind kind,
                              @Nullable GridOrigin origin) {
        this.network = network;
        this.owner = owner;
        this.kind = kind;
        this.origin = origin;
        this.refillPatterns = new ArrayList<>(Collections.nCopies(kind.inputSlots(), ItemStack.EMPTY));
        this.refillMinimumCounts = new ArrayList<>(Collections.nCopies(kind.inputSlots(), 0));
    }

    public INetwork network() {
        return network;
    }

    @Nullable
    public GridOrigin origin() {
        return origin;
    }

    public ContainerLevelAccess levelAccess() {
        return ContainerLevelAccess.create(owner.level(), owner.blockPosition());
    }

    public BlockPos virtualMenuPosition() {
        BlockPos base = owner.blockPosition().atY(owner.level().getMinBuildHeight());
        while (base.getY() < owner.level().getMaxBuildHeight()
                && owner.level().getBlockEntity(base) != null) {
            base = base.above();
        }
        return base;
    }

    public boolean isUsable(Player player) {
        return player == owner
                && !owner.hasDisconnected()
                && NetworkAccess.canUse(network, owner);
    }

    public boolean fill(AbstractContainerMenu menu, List<List<ItemStack>> options, int compareFlags) {
        if (movingItems || !kind.acceptsInputCount(options.size()) || !isUsable(owner)) {
            return false;
        }
        movingItems = true;
        try {
            returnInputsInternal(menu, true);
            List<SelectedInput> selectedInputs = new ArrayList<>();
            for (int inputIndex = 0; inputIndex < options.size(); inputIndex++) {
                int menuSlot = kind.inputSlot(inputIndex);
                SelectedInput selected = extractFirst(options.get(inputIndex), menu.getSlot(menuSlot), compareFlags);
                if (selected == null) {
                    rollback(selectedInputs.stream().map(SelectedInput::extracted).toList());
                    return false;
                }
                selectedInputs.add(selected);
            }
            for (SelectedInput selected : selectedInputs) {
                topUp(selected.extracted().stack(), selected.targetCount());
            }
            for (int inputIndex = 0; inputIndex < selectedInputs.size(); inputIndex++) {
                SelectedInput selected = selectedInputs.get(inputIndex);
                ItemStack stack = selected.extracted().stack();
                menu.getSlot(kind.inputSlot(inputIndex)).set(stack);
                ItemStack pattern = stack.copy();
                pattern.setCount(selected.targetCount());
                refillPatterns.set(inputIndex, pattern);
                refillMinimumCounts.set(inputIndex, selected.minimumCount());
            }
            menu.broadcastChanges();
            return true;
        } finally {
            movingItems = false;
        }
    }

    public void refillEmpty(AbstractContainerMenu menu) {
        if (movingItems || !isUsable(owner)) {
            return;
        }
        movingItems = true;
        try {
            List<RefillInput> refills = new ArrayList<>();
            for (int inputIndex = 0; inputIndex < refillPatterns.size(); inputIndex++) {
                ItemStack pattern = refillPatterns.get(inputIndex);
                int minimumCount = refillMinimumCounts.get(inputIndex);
                int menuSlot = kind.inputSlot(inputIndex);
                if (!pattern.isEmpty() && minimumCount > 0 && !menu.getSlot(menuSlot).hasItem()) {
                    ExtractedInput pulled = extractExact(pattern, minimumCount, IComparer.COMPARE_NBT);
                    if (pulled == null) {
                        rollback(refills.stream().map(RefillInput::extracted).toList());
                        return;
                    }
                    refills.add(new RefillInput(menuSlot, pulled, pattern.getCount()));
                }
            }
            for (RefillInput refill : refills) {
                topUp(refill.extracted().stack(), refill.targetCount());
                menu.getSlot(refill.slotIndex()).set(refill.extracted().stack());
            }
            if (!refills.isEmpty()) {
                menu.broadcastChanges();
            }
        } finally {
            movingItems = false;
        }
    }

    public boolean fillEmptyInput(AbstractContainerMenu menu, int inputIndex,
                                  List<ItemStack> options, int compareFlags) {
        if (movingItems || inputIndex < 0 || inputIndex >= kind.inputSlots()
                || options.isEmpty() || !isUsable(owner)) {
            return false;
        }
        Slot targetSlot = menu.getSlot(kind.inputSlot(inputIndex));
        if (targetSlot.hasItem()) {
            rememberInput(menu, inputIndex, 1);
            return true;
        }
        movingItems = true;
        try {
            SelectedInput selected = extractFirst(options, targetSlot, compareFlags);
            if (selected == null) {
                return false;
            }
            topUp(selected.extracted().stack(), selected.targetCount());
            targetSlot.set(selected.extracted().stack());
            ItemStack pattern = selected.extracted().stack().copy();
            pattern.setCount(selected.targetCount());
            refillPatterns.set(inputIndex, pattern);
            refillMinimumCounts.set(inputIndex, selected.minimumCount());
            menu.broadcastChanges();
            return true;
        } finally {
            movingItems = false;
        }
    }

    public void rememberInput(AbstractContainerMenu menu, int inputIndex, int minimumCount) {
        if (movingItems || inputIndex < 0 || inputIndex >= kind.inputSlots()
                || minimumCount < 1 || !isUsable(owner)) {
            return;
        }
        Slot slot = menu.getSlot(kind.inputSlot(inputIndex));
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return;
        }
        int targetCount = Math.min(stack.getMaxStackSize(), slot.getMaxStackSize(stack));
        if (targetCount < minimumCount) {
            return;
        }
        ItemStack pattern = stack.copy();
        pattern.setCount(targetCount);
        refillPatterns.set(inputIndex, pattern);
        refillMinimumCounts.set(inputIndex, minimumCount);
    }

    public void returnInputs(AbstractContainerMenu menu) {
        if (movingItems) {
            return;
        }
        movingItems = true;
        try {
            returnInputsInternal(menu, true);
        } finally {
            movingItems = false;
        }
    }

    public void suppressReturnToGrid() {
        suppressReturnToGrid = true;
    }

    public void returnInputsExcept(AbstractContainerMenu menu, int skippedInputIndex) {
        if (movingItems) {
            return;
        }
        movingItems = true;
        try {
            for (int inputIndex = 0; inputIndex < kind.inputSlots(); inputIndex++) {
                if (inputIndex == skippedInputIndex) {
                    continue;
                }
                returnInput(menu, inputIndex, true);
            }
        } finally {
            movingItems = false;
        }
    }

    public void returnToGridNow(AbstractContainerMenu menu) {
        if (menu != owner.containerMenu) {
            return;
        }
        suppressReturnToGrid = true;
        if (origin == null || !origin.reopen(owner)) {
            owner.closeContainer();
        }
    }

    public void returnToGridAfterClose() {
        if (suppressReturnToGrid || origin == null) {
            return;
        }
        owner.getServer().tell(new TickTask(owner.getServer().getTickCount() + 1, () -> {
            if (!owner.hasDisconnected() && owner.containerMenu == owner.inventoryMenu) {
                origin.reopen(owner);
            }
        }));
    }

    private void returnInputsInternal(AbstractContainerMenu menu, boolean clearPatterns) {
        for (int inputIndex = 0; inputIndex < kind.inputSlots(); inputIndex++) {
            returnInput(menu, inputIndex, clearPatterns);
        }
    }

    private void returnInput(AbstractContainerMenu menu, int inputIndex, boolean clearPattern) {
        Slot slot = menu.getSlot(kind.inputSlot(inputIndex));
        ItemStack stack = slot.getItem();
        if (!stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
            giveBack(stack);
        }
        if (clearPattern) {
            refillPatterns.set(inputIndex, ItemStack.EMPTY);
            refillMinimumCounts.set(inputIndex, 0);
        }
    }

    @Nullable
    private SelectedInput extractFirst(List<ItemStack> options, Slot targetSlot, int compareFlags) {
        for (ItemStack candidate : options) {
            if (candidate.isEmpty() || candidate.getCount() < 1 || candidate.getCount() > candidate.getMaxStackSize()) {
                continue;
            }
            ItemStack wanted = candidate.copy();
            int targetCount = Math.min(wanted.getMaxStackSize(), targetSlot.getMaxStackSize(wanted));
            if (!targetSlot.mayPlace(wanted) || wanted.getCount() > targetCount) {
                continue;
            }
            ExtractedInput extracted = extractExact(wanted, wanted.getCount(), compareFlags);
            if (extracted != null) {
                return new SelectedInput(extracted, wanted.getCount(), targetCount);
            }
        }
        return null;
    }

    @Nullable
    private ExtractedInput extractExact(ItemStack pattern, int count, int compareFlags) {
        ItemStack simulated = network.extractItem(pattern, count, compareFlags, Action.SIMULATE);
        if (simulated.getCount() == count) {
            ItemStack extracted = network.extractItem(pattern, count, compareFlags, Action.PERFORM);
            if (extracted.getCount() == count) {
                return new ExtractedInput(extracted, 0);
            }
            if (!extracted.isEmpty()) {
                giveBack(extracted);
            }
            return null;
        }

        List<ItemStack> variants = new ArrayList<>();
        if (!simulated.isEmpty()) {
            variants.add(simulated.copy());
        }
        for (ItemStack inventoryStack : owner.getInventory().items) {
            if (!inventoryStack.isEmpty()
                    && matches(inventoryStack, pattern, compareFlags)
                    && variants.stream().noneMatch(variant ->
                    ItemStack.isSameItemSameTags(variant, inventoryStack))) {
                variants.add(inventoryStack.copy());
            }
        }
        for (ItemStack variant : variants) {
            int inNetwork = network.extractItem(variant, count, IComparer.COMPARE_NBT,
                    Action.SIMULATE).getCount();
            int inInventory = countInPlayerInventory(variant);
            if (inNetwork + inInventory >= count) {
                return extractCombinedVariant(variant, count, inNetwork);
            }
        }
        return null;
    }

    private void topUp(ItemStack stack, int targetCount) {
        int requested = targetCount - stack.getCount();
        if (requested <= 0) {
            return;
        }
        ItemStack networkPulled = network.extractItem(stack, requested,
                IComparer.COMPARE_NBT, Action.PERFORM);
        if (!networkPulled.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(stack, networkPulled)) {
                giveBack(networkPulled);
                networkPulled = ItemStack.EMPTY;
            } else {
                int accepted = Math.min(requested, networkPulled.getCount());
                stack.grow(accepted);
                requested -= accepted;
                if (networkPulled.getCount() > accepted) {
                    ItemStack overflow = networkPulled.copy();
                    overflow.shrink(accepted);
                    giveBack(overflow);
                }
            }
        }
        if (requested > 0) {
            ItemStack inventoryPulled = takeFromPlayerInventory(stack, requested);
            if (!inventoryPulled.isEmpty()) {
                stack.grow(inventoryPulled.getCount());
            }
        }
    }

    @Nullable
    private ExtractedInput extractCombinedVariant(ItemStack variant, int count,
                                                  int simulatedNetworkCount) {
        int networkRequested = Math.min(count, simulatedNetworkCount);
        ItemStack networkPulled = networkRequested == 0 ? ItemStack.EMPTY
                : network.extractItem(variant, networkRequested,
                IComparer.COMPARE_NBT, Action.PERFORM);
        if (networkPulled.getCount() != networkRequested
                || (!networkPulled.isEmpty()
                && !ItemStack.isSameItemSameTags(networkPulled, variant))) {
            if (!networkPulled.isEmpty()) {
                giveBack(networkPulled);
            }
            return null;
        }

        int playerRequested = count - networkPulled.getCount();
        ItemStack playerPulled = takeFromPlayerInventory(variant, playerRequested);
        if (playerPulled.getCount() != playerRequested) {
            if (!networkPulled.isEmpty()) {
                giveBack(networkPulled);
            }
            restoreToPlayer(playerPulled);
            return null;
        }

        ItemStack combined = !networkPulled.isEmpty() ? networkPulled : playerPulled.copy();
        if (!networkPulled.isEmpty() && !playerPulled.isEmpty()) {
            combined.grow(playerPulled.getCount());
        }
        return new ExtractedInput(combined, playerPulled.getCount());
    }

    private ItemStack takeFromPlayerInventory(ItemStack pattern, int requested) {
        if (requested <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = pattern.copy();
        result.setCount(0);
        for (int slotIndex = 0;
             slotIndex < owner.getInventory().items.size() && result.getCount() < requested;
             slotIndex++) {
            ItemStack inventoryStack = owner.getInventory().items.get(slotIndex);
            if (inventoryStack.isEmpty()
                    || !ItemStack.isSameItemSameTags(inventoryStack, pattern)) {
                continue;
            }
            int taken = Math.min(requested - result.getCount(), inventoryStack.getCount());
            inventoryStack.shrink(taken);
            result.grow(taken);
            if (inventoryStack.isEmpty()) {
                owner.getInventory().items.set(slotIndex, ItemStack.EMPTY);
            }
        }
        owner.getInventory().setChanged();
        return result;
    }

    private int countInPlayerInventory(ItemStack pattern) {
        return owner.getInventory().items.stream()
                .filter(stack -> !stack.isEmpty()
                        && ItemStack.isSameItemSameTags(stack, pattern))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private static boolean matches(ItemStack stack, ItemStack pattern, int compareFlags) {
        return (compareFlags & IComparer.COMPARE_NBT) != 0
                ? ItemStack.isSameItemSameTags(stack, pattern)
                : ItemStack.isSameItem(stack, pattern);
    }

    private void rollback(List<ExtractedInput> extractedInputs) {
        for (ExtractedInput extracted : extractedInputs) {
            ItemStack stack = extracted.stack();
            int playerCount = Math.min(extracted.playerCount(), stack.getCount());
            if (playerCount > 0) {
                ItemStack playerPart = stack.copy();
                playerPart.setCount(playerCount);
                restoreToPlayer(playerPart);
            }
            int networkCount = stack.getCount() - playerCount;
            if (networkCount > 0) {
                ItemStack networkPart = stack.copy();
                networkPart.setCount(networkCount);
                giveBack(networkPart);
            }
        }
    }

    private void restoreToPlayer(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack remainder = stack.copy();
        owner.getInventory().add(remainder);
        if (!remainder.isEmpty()) {
            giveBack(remainder);
        }
    }

    private void giveBack(ItemStack original) {
        ItemStack remainder = original;
        if (network.canRun() && network.getSecurityManager().hasPermission(Permission.INSERT, owner)) {
            remainder = network.insertItem(original, original.getCount(), Action.PERFORM);
        }
        if (!remainder.isEmpty()) {
            ItemStack playerRemainder = remainder.copy();
            owner.getInventory().add(playerRemainder);
            if (!playerRemainder.isEmpty()) {
                owner.drop(playerRemainder, false, false);
            }
        }
    }

    private record ExtractedInput(ItemStack stack, int playerCount) {
    }

    private record SelectedInput(ExtractedInput extracted, int minimumCount, int targetCount) {
    }

    private record RefillInput(int slotIndex, ExtractedInput extracted, int targetCount) {
    }
}
