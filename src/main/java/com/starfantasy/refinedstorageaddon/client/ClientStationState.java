package com.starfantasy.refinedstorageaddon.client;

import com.refinedmods.refinedstorage.screen.grid.GridScreen;
import com.refinedmods.refinedstorage.screen.grid.stack.IGridStack;
import com.refinedmods.refinedstorage.screen.grid.stack.ItemGridStack;
import com.starfantasy.refinedstorageaddon.network.AddonNetwork;
import com.starfantasy.refinedstorageaddon.network.ServerboundStationSlotsRequestPacket;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import com.starfantasy.refinedstorageaddon.station.StationSlotStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class ClientStationState {
    private static final List<StationModuleSlot> SLOT_WIDGETS = new ArrayList<>();
    private static final List<StationOpenButton> OPEN_BUTTONS = new ArrayList<>();
    private static final Set<StationKind> CONFIGURED_KINDS = EnumSet.noneOf(StationKind.class);
    private static GridScreen currentGrid;
    private static List<ItemStack> stationSlots = emptySlots();
    private static Map<StackKey, Long> networkMaterialCounts = Map.of();
    private static Map<Item, List<StackKey>> networkMaterialVariants = Map.of();
    private static boolean networkMaterialCacheDirty = true;
    private static int cachedNetworkViewSize = -1;
    private static int networkMenuId = -1;
    private static StationKind networkMenuKind;

    private ClientStationState() {
    }

    public static void install(GridScreen screen, Consumer<GuiEventListener> addListener) {
        currentGrid = screen;
        SLOT_WIDGETS.clear();
        OPEN_BUTTONS.clear();
        CONFIGURED_KINDS.clear();
        stationSlots = emptySlots();
        clearNetworkMaterialCache();
        screen.getView().addDeltaListener(ignored -> networkMaterialCacheDirty = true);

        int panelWidth = 30;
        int panelHeight = 5 + StationSlotStorage.SLOT_COUNT * 18 + 5;
        int panelX = screen.getGuiLeft() + screen.getXSize() - panelWidth;
        int panelY = screen.getGuiTop() + screen.getYSize() - panelHeight;
        int firstSlotX = panelX + 5;
        int firstSlotY = panelY + 5;
        addListener.accept(new StationModulePanel(panelX, panelY,
                StationSlotStorage.SLOT_COUNT));

        for (int slotIndex = 0; slotIndex < StationSlotStorage.SLOT_COUNT; slotIndex++) {
            int y = firstSlotY + slotIndex * 18;
            StationModuleSlot slot = new StationModuleSlot(screen, slotIndex);
            slot.setX(firstSlotX);
            slot.setY(y);
            SLOT_WIDGETS.add(slot);
            addListener.accept(slot);

            StationOpenButton button = new StationOpenButton(screen, slotIndex);
            button.setX(firstSlotX - 25);
            button.setY(y);
            button.visible = false;
            OPEN_BUTTONS.add(button);
            addListener.accept(button);
        }
        requestSlots();
    }

    public static void applyStationSlots(int containerId, List<ItemStack> slots) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isCraftingGridMenuOpen(minecraft)
                || minecraft.player.containerMenu.containerId != containerId) {
            return;
        }
        List<ItemStack> updated = new ArrayList<>();
        CONFIGURED_KINDS.clear();
        for (int index = 0; index < StationSlotStorage.SLOT_COUNT; index++) {
            ItemStack stack = index < slots.size() ? slots.get(index).copy() : ItemStack.EMPTY;
            if (!stack.isEmpty()) {
                stack.setCount(1);
            }
            updated.add(stack);
            StationKind kind = StationKind.fromActivationItem(stack);
            if (kind != null) {
                CONFIGURED_KINDS.add(kind);
            }
            if (index < OPEN_BUTTONS.size()) {
                OPEN_BUTTONS.get(index).visible = kind != null;
                OPEN_BUTTONS.get(index).active = kind != null;
            }
        }
        stationSlots = List.copyOf(updated);
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean craftingGridOpen = isCraftingGridMenuOpen(minecraft);
        if (!craftingGridOpen) {
            currentGrid = null;
            SLOT_WIDGETS.clear();
            OPEN_BUTTONS.clear();
        }

        boolean networkStationOpen = minecraft.player != null
                && minecraft.player.containerMenu.containerId == networkMenuId;
        if (!networkStationOpen) {
            networkMenuId = -1;
            networkMenuKind = null;
            if (!craftingGridOpen) {
                CONFIGURED_KINDS.clear();
                stationSlots = emptySlots();
            }
        } else if (networkMenuKind != null) {
            ClientNetworkMenus.replaceActive(networkMenuId, networkMenuKind);
        }
    }

    public static void markNetworkMenu(int containerId, StationKind kind) {
        networkMenuId = containerId;
        networkMenuKind = kind;
        ClientNetworkMenus.replaceActive(containerId, kind);
    }

    public static boolean isNetworkMenu(AbstractContainerMenu menu, StationKind kind) {
        return menu.containerId == networkMenuId && networkMenuKind == kind;
    }

    public static boolean isStationAvailable(StationKind kind) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean inNetworkStation = minecraft.player != null
                && minecraft.player.containerMenu.containerId == networkMenuId
                && networkMenuKind == kind;
        return kind.isInstalled()
                && (inNetworkStation || isCraftingGridMenuOpen(minecraft)
                && CONFIGURED_KINDS.contains(kind));
    }

    public static void captureCurrentNetworkItems() {
        refreshNetworkMaterialCache();
    }

    public static boolean hasMaterials(AbstractContainerMenu menu, StationKind kind,
                                       List<List<ItemStack>> options, boolean compareNbt) {
        if (!kind.acceptsInputCount(options.size())) {
            return false;
        }
        refreshNetworkMaterialCache();
        MaterialPool pool = new MaterialPool();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.getInventory().items.stream()
                    .filter(stack -> !stack.isEmpty())
                    .forEach(pool::addDynamic);
        }
        if (isNetworkMenu(menu, kind)) {
            for (int inputIndex = 0; inputIndex < kind.inputSlots(); inputIndex++) {
                ItemStack stack = menu.getSlot(kind.inputSlot(inputIndex)).getItem();
                if (!stack.isEmpty()) {
                    pool.addDynamic(stack);
                }
            }
        }
        for (List<ItemStack> slotOptions : options) {
            boolean found = false;
            for (ItemStack option : slotOptions) {
                if (pool.consume(option, compareNbt)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    public static StationKind stationKind(int slotIndex) {
        return StationKind.fromActivationItem(slotItem(slotIndex));
    }

    public static ItemStack slotItem(int slotIndex) {
        return slotIndex >= 0 && slotIndex < stationSlots.size()
                ? stationSlots.get(slotIndex) : ItemStack.EMPTY;
    }

    public static boolean isOverAddonControl(Screen screen, double mouseX, double mouseY) {
        if (screen != currentGrid) {
            return false;
        }
        for (StationModuleSlot slot : SLOT_WIDGETS) {
            if (slot.visible && slot.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        for (StationOpenButton button : OPEN_BUTTONS) {
            if (button.visible && button.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCraftingGridMenuOpen(Minecraft minecraft) {
        return currentGrid != null
                && minecraft.player != null
                && minecraft.player.containerMenu == currentGrid.getMenu();
    }

    private static void requestSlots() {
        if (currentGrid != null) {
            AddonNetwork.CHANNEL.sendToServer(new ServerboundStationSlotsRequestPacket(
                    currentGrid.getMenu().containerId));
        }
    }

    private static void refreshNetworkMaterialCache() {
        if (currentGrid == null) {
            return;
        }
        int viewSize = currentGrid.getView().getAllStacks().size();
        if (!networkMaterialCacheDirty && viewSize == cachedNetworkViewSize) {
            return;
        }
        Map<StackKey, Long> counts = new HashMap<>();
        Map<Item, List<StackKey>> variants = new HashMap<>();
        for (IGridStack value : currentGrid.getView().getAllStacks()) {
            if (value instanceof ItemGridStack itemGridStack && value.getQuantity() > 0) {
                addMaterial(counts, variants, itemGridStack.getStack(), value.getQuantity());
            }
        }
        Map<Item, List<StackKey>> immutableVariants = new HashMap<>();
        variants.forEach((item, keys) -> immutableVariants.put(item, List.copyOf(keys)));
        networkMaterialCounts = Map.copyOf(counts);
        networkMaterialVariants = Map.copyOf(immutableVariants);
        cachedNetworkViewSize = viewSize;
        networkMaterialCacheDirty = false;
    }

    private static void clearNetworkMaterialCache() {
        networkMaterialCounts = Map.of();
        networkMaterialVariants = Map.of();
        networkMaterialCacheDirty = true;
        cachedNetworkViewSize = -1;
    }

    private static void addMaterial(Map<StackKey, Long> counts,
                                    Map<Item, List<StackKey>> variants,
                                    ItemStack stack, long count) {
        if (stack.isEmpty() || count <= 0) {
            return;
        }
        StackKey key = StackKey.of(stack);
        counts.merge(key, count, Long::sum);
        List<StackKey> itemVariants = variants.computeIfAbsent(stack.getItem(), ignored -> new ArrayList<>());
        if (!itemVariants.contains(key)) {
            itemVariants.add(key);
        }
    }

    private record StackKey(Item item, CompoundTag tag) {
        private static StackKey of(ItemStack stack) {
            return new StackKey(stack.getItem(), stack.hasTag() ? stack.getTag().copy() : null);
        }
    }

    private static final class MaterialPool {
        private final Map<StackKey, Long> dynamicCounts = new HashMap<>();
        private final Map<Item, List<StackKey>> dynamicVariants = new HashMap<>();
        private final Map<StackKey, Long> consumed = new HashMap<>();

        private void addDynamic(ItemStack stack) {
            addMaterial(dynamicCounts, dynamicVariants, stack, stack.getCount());
        }

        private boolean consume(ItemStack option, boolean compareNbt) {
            if (option.isEmpty() || option.getCount() <= 0) {
                return false;
            }
            long required = option.getCount();
            if (compareNbt) {
                return consume(StackKey.of(option), required);
            }
            List<StackKey> networkVariants = networkMaterialVariants.getOrDefault(
                    option.getItem(), List.of());
            for (StackKey key : networkVariants) {
                if (consume(key, required)) {
                    return true;
                }
            }
            List<StackKey> localVariants = dynamicVariants.getOrDefault(
                    option.getItem(), List.of());
            for (StackKey key : localVariants) {
                if (consume(key, required)) {
                    return true;
                }
            }
            return false;
        }

        private boolean consume(StackKey key, long required) {
            long available = networkMaterialCounts.getOrDefault(key, 0L)
                    + dynamicCounts.getOrDefault(key, 0L)
                    - consumed.getOrDefault(key, 0L);
            if (available < required) {
                return false;
            }
            consumed.merge(key, required, Long::sum);
            return true;
        }
    }

    private static List<ItemStack> emptySlots() {
        List<ItemStack> result = new ArrayList<>();
        for (int index = 0; index < StationSlotStorage.SLOT_COUNT; index++) {
            result.add(ItemStack.EMPTY);
        }
        return List.copyOf(result);
    }
}
