package com.starfantasy.refinedstorageaddon.station;

import com.refinedmods.refinedstorage.blockentity.BaseBlockEntity;
import com.starfantasy.refinedstorageaddon.StarFantasyRefinedStorageAddon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = StarFantasyRefinedStorageAddon.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StationSlotEvents {
    private StationSlotEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        dropStoredWorkstations(level, event.getPos());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Set<BlockPos> affected = new HashSet<>(event.getAffectedBlocks());
        affected.forEach(pos -> dropStoredWorkstations(level, pos));
    }

    private static void dropStoredWorkstations(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BaseBlockEntity baseBlockEntity)) {
            return;
        }
        StationSlotStorage.takeFromBlockEntity(baseBlockEntity).forEach(stack ->
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5,
                        pos.getZ() + 0.5, stack));
    }
}
