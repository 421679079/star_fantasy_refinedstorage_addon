package com.starfantasy.refinedstorageaddon.compat.disenchanting;

import com.chirptheboy.disenchanting.block.disenchant.TileDisenchant;

final class DisenchantingConfigCompat {
    private DisenchantingConfigCompat() {
    }

    static boolean requiresExperience() {
        return TileDisenchant.REQUIRES_EXPERIENCE == null
                || TileDisenchant.REQUIRES_EXPERIENCE.get();
    }

    static boolean randomEnchantment() {
        return TileDisenchant.RANDOM_ENCHANTMENT != null
                && TileDisenchant.RANDOM_ENCHANTMENT.get();
    }

    static boolean resetAnvilCost() {
        return TileDisenchant.RESET_ANVIL_COST != null
                && TileDisenchant.RESET_ANVIL_COST.get();
    }

    static boolean damagesItem() {
        return TileDisenchant.DAMAGE_ITEM != null && TileDisenchant.DAMAGE_ITEM.get();
    }

    static int costMultiplier() {
        return TileDisenchant.COST_SLIDER == null ? 2 : TileDisenchant.COST_SLIDER.get();
    }

    static int damagePercent() {
        return TileDisenchant.DAMAGE_PERCENT == null ? 5 : TileDisenchant.DAMAGE_PERCENT.get();
    }
}
