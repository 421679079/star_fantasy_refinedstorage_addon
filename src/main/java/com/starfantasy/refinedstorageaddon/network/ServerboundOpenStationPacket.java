package com.starfantasy.refinedstorageaddon.network;

import com.starfantasy.refinedstorageaddon.station.StationKind;
import com.starfantasy.refinedstorageaddon.station.StationMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record ServerboundOpenStationPacket(@Nullable StationKind kind,
                                           @Nullable ResourceLocation recipeId,
                                           List<List<ItemStack>> inputs) {
    private static final int MAX_INPUT_SLOTS = 16;
    private static final int MAX_OPTIONS_PER_SLOT = 64;

    public static ServerboundOpenStationPacket openOnly(StationKind kind) {
        return new ServerboundOpenStationPacket(kind, null, List.of());
    }

    public static ServerboundOpenStationPacket recipe(StationKind kind, ResourceLocation recipeId) {
        return new ServerboundOpenStationPacket(kind, recipeId, List.of());
    }

    public static ServerboundOpenStationPacket recipeWithIngredients(StationKind kind,
                                                                     ResourceLocation recipeId,
                                                                     List<List<ItemStack>> inputs) {
        return new ServerboundOpenStationPacket(kind, recipeId, inputs);
    }

    public static ServerboundOpenStationPacket ingredients(StationKind kind, List<List<ItemStack>> inputs) {
        return new ServerboundOpenStationPacket(kind, null, inputs);
    }

    public static void encode(ServerboundOpenStationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.kind == null ? 0 : packet.kind.ordinal() + 1);
        buffer.writeBoolean(packet.recipeId != null);
        if (packet.recipeId != null) {
            buffer.writeResourceLocation(packet.recipeId);
        }
        buffer.writeVarInt(Math.min(packet.inputs.size(), MAX_INPUT_SLOTS));
        for (List<ItemStack> slot : packet.inputs.stream().limit(MAX_INPUT_SLOTS).toList()) {
            buffer.writeVarInt(Math.min(slot.size(), MAX_OPTIONS_PER_SLOT));
            for (ItemStack stack : slot.stream().limit(MAX_OPTIONS_PER_SLOT).toList()) {
                buffer.writeItem(stack);
            }
        }
    }

    public static ServerboundOpenStationPacket decode(FriendlyByteBuf buffer) {
        StationKind kind = StationKind.byId(buffer.readVarInt() - 1);
        ResourceLocation recipeId = buffer.readBoolean() ? buffer.readResourceLocation() : null;
        int slotCount = buffer.readVarInt();
        if (slotCount < 0 || slotCount > MAX_INPUT_SLOTS) {
            throw new IllegalArgumentException("Too many station input slots: " + slotCount);
        }
        List<List<ItemStack>> inputs = new ArrayList<>();
        for (int slotIndex = 0; slotIndex < slotCount; slotIndex++) {
            int optionCount = buffer.readVarInt();
            if (optionCount < 0 || optionCount > MAX_OPTIONS_PER_SLOT) {
                throw new IllegalArgumentException("Too many station ingredient options: " + optionCount);
            }
            List<ItemStack> options = new ArrayList<>();
            for (int option = 0; option < optionCount; option++) {
                ItemStack stack = buffer.readItem();
                if (!stack.isEmpty() && stack.getCount() <= stack.getMaxStackSize()) {
                    options.add(stack);
                }
            }
            inputs.add(options);
        }
        return new ServerboundOpenStationPacket(kind, recipeId, inputs);
    }

    public static void handle(ServerboundOpenStationPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && packet.kind != null) {
                StationMenus.open(player, packet.kind, packet.recipeId, packet.inputs);
            }
        });
        context.setPacketHandled(true);
    }
}
