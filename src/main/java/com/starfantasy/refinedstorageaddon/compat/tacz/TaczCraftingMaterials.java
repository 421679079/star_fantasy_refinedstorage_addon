package com.starfantasy.refinedstorageaddon.compat.tacz;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.network.security.Permission;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.api.util.IComparer;
import com.refinedmods.refinedstorage.api.util.StackListEntry;
import com.starfantasy.refinedstorageaddon.network.ClientboundTaczNetworkConsumptionPacket.ConsumedStack;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class TaczCraftingMaterials {
    private TaczCraftingMaterials() {
    }

    static Result consume(INetwork network, ServerPlayer player,
                          List<GunSmithTableIngredient> ingredients) {
        if (player.isCreative()) {
            return new Result(true, List.of());
        }
        if (!network.canRun()
                || !network.getSecurityManager().hasPermission(Permission.EXTRACT, player)) {
            return Result.failure();
        }

        long totalRequired = 0;
        for (GunSmithTableIngredient ingredient : ingredients) {
            if (ingredient.getCount() < 0) {
                return Result.failure();
            }
            totalRequired += ingredient.getCount();
        }
        if (totalRequired == 0) {
            return new Result(true, List.of());
        }

        List<MaterialSource> sources = collectSources(network, player, ingredients,
                Math.min(totalRequired, Integer.MAX_VALUE));
        Allocation allocation = allocate(sources, ingredients, totalRequired);
        if (allocation == null || !preflight(network, player, sources, allocation)) {
            return Result.failure();
        }

        List<ItemStack> pulledFromNetwork = new ArrayList<>();
        List<ConsumedStack> networkConsumed = new ArrayList<>();
        for (int sourceIndex = 0; sourceIndex < sources.size(); sourceIndex++) {
            MaterialSource source = sources.get(sourceIndex);
            int count = allocation.consumed()[sourceIndex];
            if (source.playerSlot() >= 0 || count <= 0) {
                continue;
            }
            ItemStack pulled = network.extractItem(source.pattern(), count,
                    IComparer.COMPARE_NBT, Action.PERFORM);
            if (pulled.getCount() != count
                    || !ItemStack.isSameItemSameTags(pulled, source.pattern())) {
                if (!pulled.isEmpty()) {
                    pulledFromNetwork.add(pulled);
                }
                rollbackNetwork(network, player, pulledFromNetwork);
                return Result.failure();
            }
            pulledFromNetwork.add(pulled);
            networkConsumed.add(new ConsumedStack(source.pattern(), count));
        }

        for (int sourceIndex = 0; sourceIndex < sources.size(); sourceIndex++) {
            MaterialSource source = sources.get(sourceIndex);
            int count = allocation.consumed()[sourceIndex];
            if (source.playerSlot() < 0 || count <= 0) {
                continue;
            }
            ItemStack inventoryStack = player.getInventory().items.get(source.playerSlot());
            inventoryStack.shrink(count);
            if (inventoryStack.isEmpty()) {
                player.getInventory().items.set(source.playerSlot(), ItemStack.EMPTY);
            }
        }
        player.getInventory().setChanged();
        return new Result(true, List.copyOf(networkConsumed));
    }

    private static List<MaterialSource> collectSources(INetwork network, ServerPlayer player,
                                                       List<GunSmithTableIngredient> ingredients,
                                                       long totalRequired) {
        List<MaterialSource> result = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack stack = player.getInventory().items.get(slot);
            if (!stack.isEmpty() && matchesAny(stack, ingredients)) {
                result.add(new MaterialSource(stack.copy(), slot,
                        (int) Math.min(stack.getCount(), totalRequired)));
            }
        }
        for (StackListEntry<ItemStack> entry : network.getItemStorageCache().getList().getStacks()) {
            ItemStack stack = entry.getStack();
            if (stack.isEmpty() || !matchesAny(stack, ingredients)) {
                continue;
            }
            int capacity = (int) Math.min(stack.getCount(), totalRequired);
            MaterialSource existing = result.stream()
                    .filter(source -> source.playerSlot() < 0
                            && ItemStack.isSameItemSameTags(source.pattern(), stack))
                    .findFirst().orElse(null);
            if (existing == null) {
                result.add(new MaterialSource(stack.copy(), -1, capacity));
            } else {
                existing.growCapacity(capacity, totalRequired);
            }
        }
        return result;
    }

    private static boolean matchesAny(ItemStack stack,
                                      List<GunSmithTableIngredient> ingredients) {
        return ingredients.stream().anyMatch(entry -> entry.getIngredient().test(stack));
    }

    private static Allocation allocate(List<MaterialSource> sources,
                                       List<GunSmithTableIngredient> ingredients,
                                       long totalRequired) {
        int sourceNode = 0;
        int materialStart = 1;
        int ingredientStart = materialStart + sources.size();
        int sinkNode = ingredientStart + ingredients.size();
        Dinic flow = new Dinic(sinkNode + 1);
        Dinic.Edge[] materialEdges = new Dinic.Edge[sources.size()];

        for (int index = 0; index < sources.size(); index++) {
            MaterialSource source = sources.get(index);
            materialEdges[index] = flow.addEdge(sourceNode, materialStart + index,
                    source.capacity());
            for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
                if (ingredients.get(ingredientIndex).getIngredient().test(source.pattern())) {
                    flow.addEdge(materialStart + index, ingredientStart + ingredientIndex,
                            source.capacity());
                }
            }
        }
        for (int index = 0; index < ingredients.size(); index++) {
            flow.addEdge(ingredientStart + index, sinkNode, ingredients.get(index).getCount());
        }
        if (flow.maxFlow(sourceNode, sinkNode) != totalRequired) {
            return null;
        }
        int[] consumed = new int[sources.size()];
        for (int index = 0; index < sources.size(); index++) {
            consumed[index] = (int) (materialEdges[index].originalCapacity()
                    - materialEdges[index].capacity());
        }
        return new Allocation(consumed);
    }

    private static boolean preflight(INetwork network, ServerPlayer player,
                                     List<MaterialSource> sources, Allocation allocation) {
        for (int index = 0; index < sources.size(); index++) {
            MaterialSource source = sources.get(index);
            int count = allocation.consumed()[index];
            if (count <= 0) {
                continue;
            }
            if (source.playerSlot() >= 0) {
                ItemStack current = player.getInventory().items.get(source.playerSlot());
                if (!ItemStack.isSameItemSameTags(current, source.pattern())
                        || current.getCount() < count) {
                    return false;
                }
            } else {
                ItemStack simulated = network.extractItem(source.pattern(), count,
                        IComparer.COMPARE_NBT, Action.SIMULATE);
                if (simulated.getCount() != count
                        || !ItemStack.isSameItemSameTags(simulated, source.pattern())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void rollbackNetwork(INetwork network, ServerPlayer player,
                                        List<ItemStack> pulledStacks) {
        for (ItemStack pulled : pulledStacks) {
            ItemStack remainder = network.insertItem(pulled, pulled.getCount(), Action.PERFORM);
            if (!remainder.isEmpty()) {
                ItemStack playerRemainder = remainder.copy();
                player.getInventory().add(playerRemainder);
                if (!playerRemainder.isEmpty()) {
                    player.drop(playerRemainder, false, false);
                }
            }
        }
    }

    record Result(boolean success, List<ConsumedStack> networkConsumed) {
        private static Result failure() {
            return new Result(false, List.of());
        }
    }

    private record Allocation(int[] consumed) {
    }

    private static final class MaterialSource {
        private final ItemStack pattern;
        private final int playerSlot;
        private int capacity;

        private MaterialSource(ItemStack pattern, int playerSlot, int capacity) {
            this.pattern = pattern;
            this.playerSlot = playerSlot;
            this.capacity = capacity;
        }

        private ItemStack pattern() {
            return pattern;
        }

        private int playerSlot() {
            return playerSlot;
        }

        private int capacity() {
            return capacity;
        }

        private void growCapacity(int amount, long maximum) {
            capacity = (int) Math.min((long) capacity + amount, maximum);
        }
    }

    private static final class Dinic {
        private final List<List<Edge>> graph;
        private final int[] level;
        private final int[] next;

        private Dinic(int nodes) {
            graph = new ArrayList<>(nodes);
            for (int index = 0; index < nodes; index++) {
                graph.add(new ArrayList<>());
            }
            level = new int[nodes];
            next = new int[nodes];
        }

        private Edge addEdge(int from, int to, long capacity) {
            Edge forward = new Edge(to, graph.get(to).size(), capacity, capacity);
            Edge reverse = new Edge(from, graph.get(from).size(), 0, 0);
            graph.get(from).add(forward);
            graph.get(to).add(reverse);
            return forward;
        }

        private long maxFlow(int source, int sink) {
            long result = 0;
            while (buildLevels(source, sink)) {
                Arrays.fill(next, 0);
                long pushed;
                while ((pushed = push(source, sink, Long.MAX_VALUE)) > 0) {
                    result += pushed;
                }
            }
            return result;
        }

        private boolean buildLevels(int source, int sink) {
            Arrays.fill(level, -1);
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            level[source] = 0;
            queue.add(source);
            while (!queue.isEmpty()) {
                int node = queue.removeFirst();
                for (Edge edge : graph.get(node)) {
                    if (edge.capacity() > 0 && level[edge.to()] < 0) {
                        level[edge.to()] = level[node] + 1;
                        queue.addLast(edge.to());
                    }
                }
            }
            return level[sink] >= 0;
        }

        private long push(int node, int sink, long amount) {
            if (node == sink) {
                return amount;
            }
            List<Edge> edges = graph.get(node);
            for (; next[node] < edges.size(); next[node]++) {
                Edge edge = edges.get(next[node]);
                if (edge.capacity() <= 0 || level[edge.to()] != level[node] + 1) {
                    continue;
                }
                long pushed = push(edge.to(), sink, Math.min(amount, edge.capacity()));
                if (pushed <= 0) {
                    continue;
                }
                edge.shrink(pushed);
                graph.get(edge.to()).get(edge.reverseIndex()).grow(pushed);
                return pushed;
            }
            return 0;
        }

        private static final class Edge {
            private final int to;
            private final int reverseIndex;
            private final long originalCapacity;
            private long capacity;

            private Edge(int to, int reverseIndex, long capacity, long originalCapacity) {
                this.to = to;
                this.reverseIndex = reverseIndex;
                this.capacity = capacity;
                this.originalCapacity = originalCapacity;
            }

            private int to() {
                return to;
            }

            private int reverseIndex() {
                return reverseIndex;
            }

            private long originalCapacity() {
                return originalCapacity;
            }

            private long capacity() {
                return capacity;
            }

            private void shrink(long amount) {
                capacity -= amount;
            }

            private void grow(long amount) {
                capacity += amount;
            }
        }
    }
}
