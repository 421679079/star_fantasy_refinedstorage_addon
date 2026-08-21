package com.starfantasy.refinedstorageaddon.station;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

public enum StationKind {
    STONECUTTER("stonecutter", "minecraft:stonecutter", 1, 1, new int[]{0}),
    SMITHING("smithing", "minecraft:smithing_table", 3, 3, new int[]{0, 1, 2}),
    ANVIL("anvil", "minecraft:anvil", 2, 2, new int[]{0, 1},
            "minecraft:anvil", "minecraft:chipped_anvil", "minecraft:damaged_anvil"),
    GRINDSTONE("grindstone", "minecraft:grindstone", 2, 2, new int[]{0, 1}),
    GOETY_DARK_ANVIL("goety_dark_anvil", "goety:dark_anvil", 2, 2, new int[]{0, 1},
            "goety:dark_anvil", "goety:chipped_dark_anvil", "goety:damaged_dark_anvil"),
    TRANSMOGRIFICATION_TABLE("transmogrification_table", "transmog:transmogrification_table",
            3, 2, new int[]{0, 1}),
    QUALITY_REFORGING_STATION("quality_reforging_station", "quality_equipment:reforging_station",
            0, 2, new int[]{1, 2}),
    TERRA_WORKSHOP("terra_workshop", "terra_curio:workshop",
            0, 1, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}),
    DISENCHANTER("disenchanter", "disenchanting:disenchanter",
            2, 1, new int[]{1, 0}),
    IRONS_INSCRIPTION_TABLE("irons_inscription_table", "irons_spellbooks:inscription_table",
            38, 2, new int[]{36, 37}),
    IRONS_ARCANE_ANVIL("irons_arcane_anvil", "irons_spellbooks:arcane_anvil",
            2, 2, new int[]{0, 1}),
    IRONS_SCROLL_FORGE("irons_scroll_forge", "irons_spellbooks:scroll_forge",
            39, 3, new int[]{36, 37, 38}),
    TACZ_GUN_SMITH_TABLE("tacz_gun_smith_table", "tacz:gun_smith_table",
            -1, 0, new int[]{}),
    TACZ_AMMO_WORKBENCH("tacz_ammo_workbench", "tacz:workbench_a",
            -1, 0, new int[]{}),
    TACZ_ATTACHMENT_WORKBENCH("tacz_attachment_workbench", "tacz:workbench_c",
            -1, 0, new int[]{});

    private final String translationSuffix;
    private final ResourceLocation iconId;
    private final List<ResourceLocation> activationItemIds;
    private final int minimumInputSlots;
    private final int[] inputSlotIndexes;
    private final int resultSlot;

    StationKind(String translationSuffix, String iconId, int resultSlot, int minimumInputSlots,
                int[] inputSlotIndexes,
                String... activationItemIds) {
        this.translationSuffix = translationSuffix;
        this.iconId = parseId(iconId);
        this.activationItemIds = activationItemIds.length == 0
                ? List.of(this.iconId)
                : Arrays.stream(activationItemIds).map(StationKind::parseId).toList();
        this.minimumInputSlots = minimumInputSlots;
        this.inputSlotIndexes = inputSlotIndexes;
        this.resultSlot = resultSlot;
    }

    public String translationKey() {
        return "gui.star_fantasy_refinedstorage_addon." + translationSuffix;
    }

    public Component activationItemName() {
        return BuiltInRegistries.ITEM.getOptional(iconId)
                .map(item -> activationDisplayStack(new ItemStack(item)).getHoverName())
                .orElse(Component.translatable(translationKey()));
    }

    private ItemStack activationDisplayStack(ItemStack stack) {
        switch (this) {
            case TACZ_AMMO_WORKBENCH -> stack.getOrCreateTag().putString(
                    "BlockId", "tacz:ammo_workbench");
            case TACZ_ATTACHMENT_WORKBENCH -> stack.getOrCreateTag().putString(
                    "BlockId", "tacz:attachment_workbench");
            default -> {
            }
        }
        return stack;
    }

    public boolean isInstalled() {
        return BuiltInRegistries.ITEM.getOptional(iconId).isPresent();
    }

    public int inputSlots() {
        return inputSlotIndexes.length;
    }

    public int inputSlot(int inputIndex) {
        return inputSlotIndexes[inputIndex];
    }

    public boolean acceptsInputCount(int count) {
        return count >= minimumInputSlots && count <= inputSlotIndexes.length;
    }

    public int resultSlot() {
        return resultSlot;
    }

    public boolean isActivationItem(ItemStack stack) {
        return !stack.isEmpty()
                && activationItemIds.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()))
                && matchesActivationData(stack);
    }

    private boolean matchesActivationData(ItemStack stack) {
        return switch (this) {
            case TACZ_GUN_SMITH_TABLE -> !stack.hasTag()
                    || !stack.getTag().contains("BlockId")
                    || "tacz:gun_smith_table".equals(stack.getTag().getString("BlockId"));
            case TACZ_AMMO_WORKBENCH -> stack.hasTag()
                    && "tacz:ammo_workbench".equals(stack.getTag().getString("BlockId"));
            case TACZ_ATTACHMENT_WORKBENCH -> stack.hasTag()
                    && "tacz:attachment_workbench".equals(stack.getTag().getString("BlockId"));
            default -> true;
        };
    }

    public static StationKind fromActivationItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (StationKind kind : values()) {
            if (kind.isInstalled() && kind.isActivationItem(stack)) {
                return kind;
            }
        }
        return null;
    }

    public static StationKind byId(int id) {
        StationKind[] values = values();
        return id >= 0 && id < values.length ? values[id] : null;
    }

    private static ResourceLocation parseId(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            throw new IllegalArgumentException("Invalid station item id: " + value);
        }
        return id;
    }
}
