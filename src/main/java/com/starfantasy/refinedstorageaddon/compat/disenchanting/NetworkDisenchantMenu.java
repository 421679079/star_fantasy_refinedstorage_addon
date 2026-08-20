package com.starfantasy.refinedstorageaddon.compat.disenchanting;

import com.starfantasy.refinedstorageaddon.registry.AddonMenus;
import com.starfantasy.refinedstorageaddon.station.NetworkMenuSession;
import com.starfantasy.refinedstorageaddon.station.NetworkStationMenu;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class NetworkDisenchantMenu extends AbstractContainerMenu
        implements NetworkStationMenu {
    private static final int INPUT_SLOT = 0;
    private static final int BOOK_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;
    private static final int PLAYER_START = 3;
    private static final int PLAYER_END = 39;
    private static final TagKey<net.minecraft.world.item.Item> IMMUNE = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath("disenchanting", "disenchanter_immune"));
    private static final TagKey<Enchantment> BLACKLIST = TagKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath("disenchanting", "blacklisted_enchantments"));

    private final Player player;
    private final SimpleContainer inputs = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            NetworkDisenchantMenu.this.slotsChanged(this);
        }
    };
    private final ResultContainer result = new ResultContainer();
    private final DataSlot cost = DataSlot.standalone();
    private final DataSlot showCost = DataSlot.standalone();
    private final DataSlot blacklisted = DataSlot.standalone();
    @Nullable
    private final NetworkMenuSession session;

    public NetworkDisenchantMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    public NetworkDisenchantMenu(int containerId, Inventory inventory,
                                 @Nullable NetworkMenuSession session) {
        super(AddonMenus.NETWORK_DISENCHANTER.get(), containerId);
        this.player = inventory.player;
        this.session = session;

        addSlot(new Slot(inputs, INPUT_SLOT, 29, 25) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isDisenchantable(stack);
            }
        });
        addSlot(new Slot(inputs, BOOK_SLOT, 83, 25) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BOOK);
            }
        });
        addSlot(new Slot(result, 0, 137, 25) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player takingPlayer, ItemStack stack) {
                super.onTake(takingPlayer, stack);
                applyDisenchantment(takingPlayer, stack);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        11 + column * 18, 71 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 11 + column * 18, 129));
        }

        addDataSlot(cost);
        addDataSlot(showCost);
        addDataSlot(blacklisted);
    }

    @Override
    public NetworkMenuSession starFantasySession() {
        return session;
    }

    @Override
    public StationKind starFantasyKind() {
        return StationKind.DISENCHANTER;
    }

    @Override
    public boolean stillValid(Player checkingPlayer) {
        return session == null || session.isUsable(checkingPlayer);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        updateResult();
    }

    public int getCost() {
        return cost.get();
    }

    public boolean showsCost() {
        return showCost.get() != 0;
    }

    public boolean isBlacklisted() {
        return blacklisted.get() != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player movingPlayer, int index) {
        Slot slot = getSlot(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == OUTPUT_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(Items.BOOK)) {
            if (!moveItemStackTo(stack, BOOK_SLOT, BOOK_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isDisenchantable(stack)) {
            if (!moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 30) {
            if (!moveItemStackTo(stack, 30, PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_START, 30, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        if (index == OUTPUT_SLOT) {
            applyDisenchantment(movingPlayer, original);
        } else {
            slot.onTake(movingPlayer, stack);
        }
        return original;
    }

    @Override
    public void removed(Player closingPlayer) {
        if (session != null) {
            session.returnInputs(this);
            result.setItem(0, ItemStack.EMPTY);
        }
        super.removed(closingPlayer);
        if (session != null) {
            session.returnToGridAfterClose();
        }
    }

    private void updateResult() {
        if (player.level().isClientSide) {
            return;
        }
        ItemStack input = inputs.getItem(INPUT_SLOT);
        if (!isDisenchantable(input) || !inputs.getItem(BOOK_SLOT).is(Items.BOOK)) {
            clearResult();
            return;
        }

        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(input);
        Enchantment selected = selectEnchantment(enchantments);
        if (selected == null || ForgeRegistries.ENCHANTMENTS.tags().getTag(BLACKLIST)
                .contains(selected)) {
            blacklisted.set(selected == null ? 0 : 1);
            cost.set(0);
            showCost.set(0);
            result.setItem(0, ItemStack.EMPTY);
            broadcastChanges();
            return;
        }

        int requiredLevels = calculateCost(selected, enchantments.getOrDefault(selected, 1));
        boolean requiresExperience = DisenchantingConfigCompat.requiresExperience();
        boolean enoughExperience = !requiresExperience || player.isCreative()
                || player.experienceLevel >= requiredLevels;
        blacklisted.set(0);
        cost.set(requiredLevels);
        showCost.set(requiresExperience && !player.isCreative() ? 1 : 0);
        if (enoughExperience) {
            ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantmentHelper.setEnchantments(Map.of(selected,
                    enchantments.getOrDefault(selected, 1)), enchantedBook);
            result.setItem(0, enchantedBook);
        } else {
            result.setItem(0, ItemStack.EMPTY);
        }
        broadcastChanges();
    }

    private void applyDisenchantment(Player takingPlayer, ItemStack enchantedBook) {
        if (takingPlayer.level().isClientSide) {
            return;
        }
        ItemStack input = inputs.getItem(INPUT_SLOT);
        ItemStack books = inputs.getItem(BOOK_SLOT);
        Map<Enchantment, Integer> outputEnchantments = EnchantmentHelper.getEnchantments(
                enchantedBook);
        if (!isDisenchantable(input) || !books.is(Items.BOOK)
                || outputEnchantments.isEmpty()) {
            updateResult();
            return;
        }

        Enchantment removedEnchantment = outputEnchantments.keySet().iterator().next();
        Map<Enchantment, Integer> remaining = new LinkedHashMap<>(
                EnchantmentHelper.getEnchantments(input));
        int originalEnchantmentCount = remaining.size();
        if (remaining.remove(removedEnchantment) == null) {
            updateResult();
            return;
        }

        if (DisenchantingConfigCompat.requiresExperience() && !takingPlayer.isCreative()) {
            takingPlayer.giveExperienceLevels(-cost.get());
        }

        ItemStack changedItem = input.copy();
        if (DisenchantingConfigCompat.resetAnvilCost() && originalEnchantmentCount == 1) {
            changedItem.setRepairCost(0);
        }
        EnchantmentHelper.setEnchantments(remaining, changedItem);

        boolean itemBroke = false;
        if (DisenchantingConfigCompat.damagesItem() && changedItem.isDamageableItem()) {
            int damage = (int) (changedItem.getMaxDamage()
                    * (DisenchantingConfigCompat.damagePercent() / 100.0D));
            int remainingDurability = changedItem.getMaxDamage() - changedItem.getDamageValue()
                    - damage;
            if (remainingDurability < 1) {
                changedItem = new ItemStack(Items.STICK);
                itemBroke = true;
            } else {
                changedItem.setDamageValue(changedItem.getDamageValue() + damage);
            }
        }

        if (input.is(Items.ENCHANTED_BOOK)) {
            changedItem = remaining.isEmpty()
                    ? new ItemStack(Items.BOOK)
                    : new ItemStack(Items.ENCHANTED_BOOK);
            if (!remaining.isEmpty()) {
                EnchantmentHelper.setEnchantments(remaining, changedItem);
            }
        }

        inputs.setItem(INPUT_SLOT, changedItem);
        inputs.removeItem(BOOK_SLOT, 1);
        takingPlayer.level().playSound(null, takingPlayer.blockPosition(),
                itemBroke ? SoundEvents.ITEM_BREAK : SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.BLOCKS, 1.0F, 1.25F);
        if (session != null) {
            session.refillEmpty(this);
        }
        updateResult();
    }

    private void clearResult() {
        blacklisted.set(0);
        cost.set(0);
        showCost.set(0);
        result.setItem(0, ItemStack.EMPTY);
        broadcastChanges();
    }

    private static boolean isDisenchantable(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(IMMUNE)
                && !EnchantmentHelper.getEnchantments(stack).isEmpty();
    }

    @Nullable
    private static Enchantment selectEnchantment(Map<Enchantment, Integer> enchantments) {
        if (enchantments.isEmpty()) {
            return null;
        }
        List<Enchantment> choices = new ArrayList<>(enchantments.keySet());
        int index = DisenchantingConfigCompat.randomEnchantment()
                ? ThreadLocalRandom.current().nextInt(choices.size()) : 0;
        return choices.get(index);
    }

    private static int calculateCost(Enchantment enchantment, int level) {
        int rarityWeight = enchantment.getRarity().getWeight();
        double levelRatio = level / (double) enchantment.getMaxLevel();
        return (int) ((1.8D - rarityWeight * 0.03D * levelRatio)
                * DisenchantingConfigCompat.costMultiplier());
    }
}
