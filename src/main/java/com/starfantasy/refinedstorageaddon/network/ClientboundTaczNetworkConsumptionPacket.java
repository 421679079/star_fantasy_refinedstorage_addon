package com.starfantasy.refinedstorageaddon.network;

import com.starfantasy.refinedstorageaddon.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record ClientboundTaczNetworkConsumptionPacket(List<ConsumedStack> consumed) {
    private static final int MAX_ENTRIES = 256;

    public ClientboundTaczNetworkConsumptionPacket {
        consumed = List.copyOf(consumed);
    }

    public static void encode(ClientboundTaczNetworkConsumptionPacket packet,
                              FriendlyByteBuf buffer) {
        int size = Math.min(packet.consumed.size(), MAX_ENTRIES);
        buffer.writeVarInt(size);
        for (int index = 0; index < size; index++) {
            ConsumedStack consumed = packet.consumed.get(index);
            ItemStack pattern = consumed.pattern.copy();
            pattern.setCount(1);
            buffer.writeItem(pattern);
            buffer.writeVarInt(consumed.count);
        }
    }

    public static ClientboundTaczNetworkConsumptionPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid TACZ material entry count: " + size);
        }
        List<ConsumedStack> consumed = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            ItemStack pattern = buffer.readItem();
            int count = buffer.readVarInt();
            if (pattern.isEmpty() || count <= 0) {
                throw new IllegalArgumentException("Invalid TACZ material consumption");
            }
            consumed.add(new ConsumedStack(pattern, count));
        }
        return new ClientboundTaczNetworkConsumptionPacket(consumed);
    }

    public static void handle(ClientboundTaczNetworkConsumptionPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.consumeTaczNetworkMaterials(packet.consumed)));
        context.setPacketHandled(true);
    }

    public record ConsumedStack(ItemStack pattern, int count) {
        public ConsumedStack {
            pattern = pattern.copy();
        }
    }
}
