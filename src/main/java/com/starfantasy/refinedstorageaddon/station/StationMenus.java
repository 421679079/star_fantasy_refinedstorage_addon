package com.starfantasy.refinedstorageaddon.station;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.util.IComparer;
import com.refinedmods.refinedstorage.container.GridContainerMenu;
import com.starfantasy.refinedstorageaddon.compat.goety.GoetyStationCompat;
import com.starfantasy.refinedstorageaddon.compat.disenchanting.NetworkDisenchantMenu;
import com.starfantasy.refinedstorageaddon.compat.irons.IronsSpellbooksStationCompat;
import com.starfantasy.refinedstorageaddon.compat.quality.QualityEquipmentStationCompat;
import com.starfantasy.refinedstorageaddon.compat.terracurio.TerraCurioStationCompat;
import com.starfantasy.refinedstorageaddon.compat.transmog.TransmogStationCompat;
import com.starfantasy.refinedstorageaddon.compat.tacz.TaczStationCompat;
import com.starfantasy.refinedstorageaddon.network.AddonNetwork;
import com.starfantasy.refinedstorageaddon.network.ClientboundStationMenuPacket;
import com.starfantasy.refinedstorageaddon.station.menu.NetworkAnvilMenu;
import com.starfantasy.refinedstorageaddon.station.menu.NetworkGrindstoneMenu;
import com.starfantasy.refinedstorageaddon.station.menu.NetworkSmithingMenu;
import com.starfantasy.refinedstorageaddon.station.menu.NetworkStonecutterMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class StationMenus {
    private StationMenus() {
    }

    public static void open(ServerPlayer player, StationKind kind, @Nullable ResourceLocation recipeId,
                            List<List<ItemStack>> clientOptions) {
        INetwork network = NetworkAccess.fromMenu(player.containerMenu);
        if (network == null || !NetworkAccess.canUse(network, player)
                || !isConfigured(player.containerMenu, kind)) {
            player.displayClientMessage(Component.translatable(
                    "message.star_fantasy_refinedstorage_addon.unavailable"), true);
            return;
        }

        TransferPlan plan = resolvePlan(player, kind, recipeId, clientOptions);
        if (plan == null) {
            player.displayClientMessage(Component.translatable(
                    "message.star_fantasy_refinedstorage_addon.invalid_recipe"), true);
            return;
        }

        if (player.containerMenu instanceof NetworkStationMenu current
                && current.starFantasyKind() == kind) {
            if (!plan.options().isEmpty()) {
                if (!current.starFantasySession().fill(player.containerMenu,
                        plan.options(), plan.compareFlags())) {
                    player.displayClientMessage(Component.translatable(
                            "message.star_fantasy_refinedstorage_addon.missing_materials"), true);
                } else {
                    finishTransfer(player.containerMenu, player, kind, plan);
                }
            }
            return;
        }

        GridOrigin origin = GridOrigin.capture(player, player.containerMenu);
        if (player.containerMenu instanceof NetworkStationMenu current) {
            current.starFantasySession().suppressReturnToGrid();
        }
        NetworkMenuSession session = new NetworkMenuSession(network, player, kind, origin);
        AtomicReference<AbstractContainerMenu> openedMenu = new AtomicReference<>();
        Component title = Component.translatable(kind.translationKey());
        if (isTaczStation(kind)) {
            TaczStationCompat.openMenu(player, session, kind, title);
            openedMenu.set(player.containerMenu);
        } else {
            player.openMenu(new SimpleMenuProvider((containerId, inventory, ignored) -> {
                AbstractContainerMenu menu = switch (kind) {
                    case STONECUTTER -> new NetworkStonecutterMenu(containerId, inventory, session);
                    case SMITHING -> new NetworkSmithingMenu(containerId, inventory, session);
                    case ANVIL -> new NetworkAnvilMenu(containerId, inventory, session);
                    case GRINDSTONE -> new NetworkGrindstoneMenu(containerId, inventory, session);
                    case GOETY_DARK_ANVIL -> GoetyStationCompat.createDarkAnvilMenu(
                            containerId, inventory, session);
                    case TRANSMOGRIFICATION_TABLE -> TransmogStationCompat.createMenu(
                            containerId, inventory, session);
                    case QUALITY_REFORGING_STATION -> QualityEquipmentStationCompat.createMenu(
                            containerId, inventory, session);
                    case TERRA_WORKSHOP -> TerraCurioStationCompat.createMenu(
                            containerId, inventory, session);
                    case DISENCHANTER -> new NetworkDisenchantMenu(containerId, inventory, session);
                    case IRONS_INSCRIPTION_TABLE -> IronsSpellbooksStationCompat.createInscriptionMenu(
                            containerId, inventory, session);
                    case IRONS_ARCANE_ANVIL -> IronsSpellbooksStationCompat.createArcaneAnvilMenu(
                            containerId, inventory, session);
                    case IRONS_SCROLL_FORGE -> IronsSpellbooksStationCompat.createScrollForgeMenu(
                            containerId, inventory, session);
                    case TACZ_GUN_SMITH_TABLE, TACZ_AMMO_WORKBENCH,
                            TACZ_ATTACHMENT_WORKBENCH -> throw new IllegalStateException(
                            "TACZ workstations require an extended menu open packet");
                };
                openedMenu.set(menu);
                return menu;
            }, title));
        }

        AbstractContainerMenu menu = openedMenu.get();
        if (menu == null || player.containerMenu != menu) {
            return;
        }
        AddonNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ClientboundStationMenuPacket(menu.containerId, kind));
        if (kind == StationKind.IRONS_SCROLL_FORGE) {
            IronsSpellbooksStationCompat.prepareOnOpen(menu, session, kind);
        }
        if (!plan.options().isEmpty()) {
            if (!session.fill(menu, plan.options(), plan.compareFlags())) {
                player.displayClientMessage(Component.translatable(
                        "message.star_fantasy_refinedstorage_addon.missing_materials"), true);
            } else {
                finishTransfer(menu, player, kind, plan);
            }
        }
    }

    private static boolean isTaczStation(StationKind kind) {
        return kind == StationKind.TACZ_GUN_SMITH_TABLE
                || kind == StationKind.TACZ_AMMO_WORKBENCH
                || kind == StationKind.TACZ_ATTACHMENT_WORKBENCH;
    }

    @Nullable
    private static TransferPlan resolvePlan(ServerPlayer player, StationKind kind,
                                            @Nullable ResourceLocation recipeId,
                                            List<List<ItemStack>> clientOptions) {
        if (recipeId == null) {
            if (clientOptions.isEmpty()) {
                if (kind == StationKind.DISENCHANTER) {
                    return new TransferPlan(List.of(List.of(new ItemStack(Items.BOOK))),
                            IComparer.COMPARE_NBT, ItemStack.EMPTY);
                }
                return new TransferPlan(List.of(), IComparer.COMPARE_NBT, ItemStack.EMPTY);
            }
            List<List<ItemStack>> validated = kind == StationKind.ANVIL
                    || kind == StationKind.GOETY_DARK_ANVIL
                    || kind == StationKind.GRINDSTONE
                    || kind == StationKind.IRONS_ARCANE_ANVIL
                    || kind == StationKind.IRONS_SCROLL_FORGE
                    ? validateClientOptions(kind, clientOptions) : null;
            return validated == null ? null
                    : new TransferPlan(validated, IComparer.COMPARE_NBT, ItemStack.EMPTY);
        }
        Recipe<?> recipe = player.serverLevel().getRecipeManager().byKey(recipeId).orElse(null);
        if (recipe == null) {
            return null;
        }
        if (kind == StationKind.STONECUTTER && !(recipe instanceof StonecutterRecipe)) {
            return null;
        }
        if (kind == StationKind.SMITHING && (!(recipe instanceof SmithingRecipe)
                || recipe.getType() != RecipeType.SMITHING)) {
            return null;
        }
        if (kind == StationKind.TERRA_WORKSHOP
                && !ResourceLocation.fromNamespaceAndPath("terra_curio", "workshop_type")
                .equals(BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()))) {
            return null;
        }
        if (kind != StationKind.STONECUTTER && kind != StationKind.SMITHING
                && kind != StationKind.TERRA_WORKSHOP) {
            return null;
        }
        if (kind == StationKind.SMITHING) {
            return resolveSmithingPlan((SmithingRecipe) recipe, clientOptions);
        }
        List<Ingredient> ingredients = recipe.getIngredients();
        if (!kind.acceptsInputCount(ingredients.size())) {
            return null;
        }
        List<List<ItemStack>> plan = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            List<ItemStack> candidates = Arrays.stream(ingredient.getItems())
                    .filter(stack -> !stack.isEmpty())
                    .limit(64)
                    .map(ItemStack::copy)
                    .toList();
            if (candidates.isEmpty()) {
                return null;
            }
            plan.add(candidates);
        }
        ItemStack expectedResult = kind == StationKind.TERRA_WORKSHOP
                ? recipe.getResultItem(player.level().registryAccess()).copy()
                : ItemStack.EMPTY;
        return new TransferPlan(plan, 0, expectedResult);
    }

    @Nullable
    private static TransferPlan resolveSmithingPlan(SmithingRecipe recipe,
                                                    List<List<ItemStack>> clientOptions) {
        List<List<ItemStack>> validated = validateClientOptions(StationKind.SMITHING, clientOptions);
        if (validated == null) {
            return null;
        }
        List<List<ItemStack>> plan = new ArrayList<>();
        for (int slot = 0; slot < StationKind.SMITHING.inputSlots(); slot++) {
            final int slotIndex = slot;
            List<ItemStack> candidates = validated.get(slot).stream()
                    .filter(stack -> switch (slotIndex) {
                        case 0 -> recipe.isTemplateIngredient(stack);
                        case 1 -> recipe.isBaseIngredient(stack);
                        case 2 -> recipe.isAdditionIngredient(stack);
                        default -> false;
                    })
                    .map(ItemStack::copy)
                    .toList();
            if (candidates.isEmpty()) {
                return null;
            }
            plan.add(candidates);
        }
        return new TransferPlan(plan, 0, ItemStack.EMPTY);
    }

    @Nullable
    private static List<List<ItemStack>> validateClientOptions(StationKind kind,
                                                               List<List<ItemStack>> clientOptions) {
        if (!kind.acceptsInputCount(clientOptions.size())) {
            return null;
        }
        List<List<ItemStack>> result = new ArrayList<>();
        for (List<ItemStack> slot : clientOptions) {
            if (slot.isEmpty() || slot.size() > 64) {
                return null;
            }
            List<ItemStack> copies = slot.stream()
                    .filter(stack -> !stack.isEmpty() && stack.getCount() > 0
                            && stack.getCount() <= stack.getMaxStackSize())
                    .map(ItemStack::copy)
                    .toList();
            if (copies.isEmpty()) {
                return null;
            }
            result.add(copies);
        }
        return result;
    }

    private static void finishTransfer(AbstractContainerMenu menu, ServerPlayer player,
                                       StationKind kind, TransferPlan plan) {
        if (kind == StationKind.TERRA_WORKSHOP && !plan.expectedResult().isEmpty()) {
            TerraCurioStationCompat.selectRecipe(menu, player, plan.expectedResult());
        }
    }

    private static boolean isConfigured(AbstractContainerMenu menu, StationKind kind) {
        if (menu instanceof GridContainerMenu gridMenu) {
            return StationSlotStorage.hasKind(gridMenu, kind);
        }
        return menu instanceof NetworkStationMenu stationMenu
                && stationMenu.starFantasyKind() == kind;
    }

    private record TransferPlan(List<List<ItemStack>> options, int compareFlags,
                                ItemStack expectedResult) {
    }
}
