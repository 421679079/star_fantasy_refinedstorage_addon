package com.starfantasy.refinedstorageaddon.compat.irons;

import com.starfantasy.refinedstorageaddon.registry.AddonMenus;
import com.starfantasy.refinedstorageaddon.station.NetworkMenuSession;
import com.starfantasy.refinedstorageaddon.station.NetworkStationMenu;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.block.scroll_forge.ScrollForgeTile;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public final class NetworkScrollForgeMenu extends ScrollForgeMenu implements NetworkStationMenu {
    private static final ResourceLocation SCROLL_FORGE_ID = ResourceLocation.fromNamespaceAndPath(
            "irons_spellbooks", "scroll_forge");

    @Nullable
    private final NetworkMenuSession session;
    private final VirtualScrollForgeTile virtualTile;
    private AbstractSpell selectedSpell = SpellRegistry.none();

    public NetworkScrollForgeMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, new VirtualScrollForgeTile(virtualPosition(inventory)));
    }

    public NetworkScrollForgeMenu(int containerId, Inventory inventory,
                                  NetworkMenuSession session) {
        this(containerId, inventory, session, new VirtualScrollForgeTile(virtualPosition(inventory)));
    }

    private NetworkScrollForgeMenu(int containerId, Inventory inventory,
                                   @Nullable NetworkMenuSession session,
                                   VirtualScrollForgeTile virtualTile) {
        super(containerId, inventory, virtualTile);
        this.session = session;
        this.virtualTile = virtualTile;
        virtualTile.attach(this);
    }

    @Override
    public MenuType<?> getType() {
        return AddonMenus.NETWORK_IRONS_SCROLL_FORGE.get();
    }

    @Override
    public NetworkMenuSession starFantasySession() {
        return session;
    }

    @Override
    public StationKind starFantasyKind() {
        return StationKind.IRONS_SCROLL_FORGE;
    }

    @Override
    public boolean stillValid(Player player) {
        return session == null || session.isUsable(player);
    }

    public void selectSpell(Player player, String spellId) {
        AbstractSpell spell = SpellRegistry.getSpell(spellId);
        if (spell != null && (spell.equals(SpellRegistry.none())
                || spell.isEnabled() && spell.canBeCraftedBy(player))) {
            selectedSpell = spell;
            setRecipeSpell(spell);
            broadcastChanges();
        }
    }

    public void takeResult(ServerPlayer player, String spellId, boolean quickMove) {
        if (session == null || player.containerMenu != this || !session.isUsable(player)) {
            return;
        }

        // The Iron screen can retain a client-side preview after its original block-position
        // packet is ignored by a virtual station. Rebuild the result authoritatively from the
        // requested spell and the current server-side inputs before taking anything.
        selectSpell(player, spellId);
        Slot resultSlot = getSlot(StationKind.IRONS_SCROLL_FORGE.resultSlot());
        ItemStack result = resultSlot.getItem().copy();
        if (result.isEmpty()) {
            sendAllDataToRemote();
            return;
        }

        ItemStack carried = getCarried();
        if (!quickMove && !carried.isEmpty()
                && (!ItemStack.isSameItemSameTags(carried, result)
                || carried.getCount() + result.getCount() > carried.getMaxStackSize())) {
            sendAllDataToRemote();
            return;
        }

        ItemStack taken = resultSlot.remove(result.getCount());
        if (taken.isEmpty()) {
            sendAllDataToRemote();
            return;
        }
        ItemStack achievementStack = taken.copy();
        if (quickMove) {
            if (!moveItemStackTo(taken, 0, 36, true) || !taken.isEmpty()) {
                resultSlot.set(achievementStack);
                sendAllDataToRemote();
                return;
            }
        } else if (carried.isEmpty()) {
            setCarried(taken);
        } else {
            carried.grow(taken.getCount());
            setCarried(carried);
        }

        resultSlot.onTake(player, achievementStack);
        session.refillEmpty(this);
        restoreSelectedRecipe();
        broadcastChanges();
        sendAllDataToRemote();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        rememberPaper();
        boolean tookResult = slotId == StationKind.IRONS_SCROLL_FORGE.resultSlot()
                && getSlot(slotId).hasItem();
        int[] countsBefore = tookResult ? inputCounts() : null;
        super.clicked(slotId, button, clickType, player);
        if (tookResult && session != null && inputsConsumed(countsBefore)) {
            session.refillEmpty(this);
            restoreSelectedRecipe();
        }
        rememberPaper();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        rememberPaper();
        boolean tookResult = index == StationKind.IRONS_SCROLL_FORGE.resultSlot()
                && getSlot(index).hasItem();
        int[] countsBefore = tookResult ? inputCounts() : null;
        ItemStack moved = super.quickMoveStack(player, index);
        if (tookResult && session != null && inputsConsumed(countsBefore)) {
            session.refillEmpty(this);
            restoreSelectedRecipe();
        }
        rememberPaper();
        return moved;
    }

    @Override
    public void removed(Player player) {
        if (session != null) {
            session.returnInputs(this);
        }
        super.removed(player);
        virtualTile.clearContent();
        if (session != null) {
            session.returnToGridAfterClose();
        }
    }

    private void rememberPaper() {
        if (session != null) {
            session.rememberInput(this, 1, 1);
        }
    }

    private void restoreSelectedRecipe() {
        if (!selectedSpell.equals(SpellRegistry.none())) {
            setRecipeSpell(selectedSpell);
            broadcastChanges();
        }
    }

    private int[] inputCounts() {
        int[] counts = new int[StationKind.IRONS_SCROLL_FORGE.inputSlots()];
        for (int inputIndex = 0; inputIndex < counts.length; inputIndex++) {
            counts[inputIndex] = getSlot(
                    StationKind.IRONS_SCROLL_FORGE.inputSlot(inputIndex)).getItem().getCount();
        }
        return counts;
    }

    private boolean inputsConsumed(int[] before) {
        if (before == null) {
            return false;
        }
        for (int inputIndex = 0; inputIndex < before.length; inputIndex++) {
            int after = getSlot(StationKind.IRONS_SCROLL_FORGE.inputSlot(inputIndex))
                    .getItem().getCount();
            if (before[inputIndex] < 1 || after >= before[inputIndex]) {
                return false;
            }
        }
        return true;
    }

    private static BlockPos virtualPosition(Inventory inventory) {
        // Iron's original screen always sends its own selection packet to this position. Keeping
        // the virtual tile one block below the dimension prevents that packet from ever touching
        // an unrelated real scroll forge; our own packet targets the player's current menu.
        return new BlockPos(0, inventory.player.level().getMinBuildHeight() - 1, 0);
    }

    private static final class VirtualScrollForgeTile extends ScrollForgeTile {
        private final ItemStackHandler handler = new ItemStackHandler(4) {
            @Override
            protected void onContentsChanged(int slot) {
                if (menu != null) {
                    menu.onSlotsChanged(slot);
                }
            }
        };
        @Nullable
        private NetworkScrollForgeMenu menu;

        private VirtualScrollForgeTile(BlockPos position) {
            super(position, BuiltInRegistries.BLOCK.get(SCROLL_FORGE_ID).defaultBlockState());
        }

        private void attach(NetworkScrollForgeMenu menu) {
            this.menu = menu;
        }

        @Override
        public ItemStackHandler getItemHandler() {
            return handler;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public void setRecipeSpell(String spellId) {
            if (menu != null) {
                AbstractSpell spell = SpellRegistry.getSpell(spellId);
                if (spell != null) {
                    menu.selectedSpell = spell;
                    menu.setRecipeSpell(spell);
                    menu.broadcastChanges();
                }
            }
        }

        @Override
        public void clearContent() {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                handler.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }
}
