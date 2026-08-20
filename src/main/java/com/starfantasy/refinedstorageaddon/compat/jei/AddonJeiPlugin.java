package com.starfantasy.refinedstorageaddon.compat.jei;

import com.refinedmods.refinedstorage.container.GridContainerMenu;
import com.starfantasy.refinedstorageaddon.StarFantasyRefinedStorageAddon;
import com.starfantasy.refinedstorageaddon.client.ClientStationState;
import com.starfantasy.refinedstorageaddon.compat.goety.client.GoetyClientNetworkMenus.NetworkDarkAnvilClientMenu;
import com.starfantasy.refinedstorageaddon.compat.terracurio.client.TerraCurioClientNetworkMenus.NetworkWorkshopClientMenu;
import com.starfantasy.refinedstorageaddon.compat.irons.NetworkArcaneAnvilMenu;
import com.starfantasy.refinedstorageaddon.compat.irons.NetworkScrollForgeMenu;
import com.starfantasy.refinedstorageaddon.client.ClientNetworkMenus.NetworkAnvilClientMenu;
import com.starfantasy.refinedstorageaddon.client.ClientNetworkMenus.NetworkSmithingClientMenu;
import com.starfantasy.refinedstorageaddon.client.ClientNetworkMenus.NetworkStonecutterClientMenu;
import com.starfantasy.refinedstorageaddon.network.AddonNetwork;
import com.starfantasy.refinedstorageaddon.network.ServerboundOpenStationPacket;
import com.starfantasy.refinedstorageaddon.network.ServerboundSelectIronsScrollSpellPacket;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.confluence.terra_curio.integration.jei.WorkshopCategory;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.jei.ArcaneAnvilJeiRecipe;
import io.redspace.ironsspellbooks.jei.ArcaneAnvilRecipeCategory;
import io.redspace.ironsspellbooks.jei.ScrollForgeRecipe;
import io.redspace.ironsspellbooks.jei.ScrollForgeRecipeCategory;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

@JeiPlugin
public final class AddonJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            StarFantasyRefinedStorageAddon.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        IRecipeTransferHandlerHelper helper = registration.getTransferHelper();
        registration.addRecipeTransferHandler(
                new RecipeIdTransferHandler<>(GridContainerMenu.class,
                        RecipeTypes.STONECUTTING, StationKind.STONECUTTER, helper),
                RecipeTypes.STONECUTTING);
        registration.addRecipeTransferHandler(
                new RecipeIdTransferHandler<>(GridContainerMenu.class,
                        RecipeTypes.SMITHING, StationKind.SMITHING, helper),
                RecipeTypes.SMITHING);
        registration.addRecipeTransferHandler(
                new AnvilTransferHandler<>(GridContainerMenu.class, null, helper), RecipeTypes.ANVIL);

        registration.addRecipeTransferHandler(
                new RecipeIdTransferHandler<>(NetworkStonecutterClientMenu.class,
                        RecipeTypes.STONECUTTING, StationKind.STONECUTTER, helper),
                RecipeTypes.STONECUTTING);
        registration.addRecipeTransferHandler(
                new RecipeIdTransferHandler<>(NetworkSmithingClientMenu.class,
                        RecipeTypes.SMITHING, StationKind.SMITHING, helper),
                RecipeTypes.SMITHING);
        registration.addRecipeTransferHandler(
                new AnvilTransferHandler<>(NetworkAnvilClientMenu.class,
                        StationKind.ANVIL, helper), RecipeTypes.ANVIL);
        if (StationKind.GOETY_DARK_ANVIL.isInstalled()) {
            registration.addRecipeTransferHandler(
                    new AnvilTransferHandler<>(NetworkDarkAnvilClientMenu.class,
                            StationKind.GOETY_DARK_ANVIL, helper), RecipeTypes.ANVIL);
        }
        if (StationKind.TERRA_WORKSHOP.isInstalled()) {
            registration.addRecipeTransferHandler(
                    new RecipeIdTransferHandler<>(GridContainerMenu.class,
                            WorkshopCategory.TYPE, StationKind.TERRA_WORKSHOP, helper),
                    WorkshopCategory.TYPE);
            registration.addRecipeTransferHandler(
                    new RecipeIdTransferHandler<>(NetworkWorkshopClientMenu.class,
                            WorkshopCategory.TYPE, StationKind.TERRA_WORKSHOP, helper),
                    WorkshopCategory.TYPE);
        }
        if (StationKind.IRONS_INSCRIPTION_TABLE.isInstalled()) {
            registration.addRecipeTransferHandler(
                    new IronsArcaneAnvilTransferHandler<>(GridContainerMenu.class, helper),
                    ArcaneAnvilRecipeCategory.ARCANE_ANVIL_RECIPE_RECIPE_TYPE);
            registration.addRecipeTransferHandler(
                    new IronsArcaneAnvilTransferHandler<>(NetworkArcaneAnvilMenu.class, helper),
                    ArcaneAnvilRecipeCategory.ARCANE_ANVIL_RECIPE_RECIPE_TYPE);
            registration.addRecipeTransferHandler(
                    new IronsScrollForgeTransferHandler<>(GridContainerMenu.class, helper),
                    ScrollForgeRecipeCategory.SCROLL_FORGE_RECIPE_RECIPE_TYPE);
            registration.addRecipeTransferHandler(
                    new IronsScrollForgeTransferHandler<>(NetworkScrollForgeMenu.class, helper),
                    ScrollForgeRecipeCategory.SCROLL_FORGE_RECIPE_RECIPE_TYPE);
        }
    }

    private abstract static class StationTransferHandler<C extends AbstractContainerMenu, R>
            implements IRecipeTransferHandler<C, R> {
        private final Class<? extends C> containerClass;
        private final RecipeType<R> recipeType;
        private final IRecipeTransferHandlerHelper helper;

        private StationTransferHandler(Class<? extends C> containerClass, RecipeType<R> recipeType,
                                       IRecipeTransferHandlerHelper helper) {
            this.containerClass = containerClass;
            this.recipeType = recipeType;
            this.helper = helper;
        }

        @Override
        public Class<? extends C> getContainerClass() {
            return containerClass;
        }

        @Override
        public Optional<MenuType<C>> getMenuType() {
            return Optional.empty();
        }

        @Override
        public RecipeType<R> getRecipeType() {
            return recipeType;
        }

        protected IRecipeTransferError missingMaterials() {
            return helper.createUserErrorWithTooltip(Component.translatable(
                    "message.star_fantasy_refinedstorage_addon.missing_materials"));
        }

        protected IRecipeTransferError unavailableStation() {
            return helper.createUserErrorWithTooltip(Component.translatable(
                    "message.star_fantasy_refinedstorage_addon.unavailable"));
        }
    }

    private static final class RecipeIdTransferHandler<C extends AbstractContainerMenu, R extends Recipe<?>>
            extends StationTransferHandler<C, R> {
        private final StationKind kind;

        private RecipeIdTransferHandler(Class<? extends C> containerClass,
                                        RecipeType<R> recipeType, StationKind kind,
                                        IRecipeTransferHandlerHelper helper) {
            super(containerClass, recipeType, helper);
            this.kind = kind;
        }

        @Override
        public IRecipeTransferError transferRecipe(C container, R recipe,
                                                   IRecipeSlotsView recipeSlots, Player player,
                                                   boolean maxTransfer, boolean doTransfer) {
            List<List<ItemStack>> inputs = inputOptions(recipeSlots, kind);
            if (!ClientStationState.isStationAvailable(kind)) {
                return unavailableStation();
            }
            if (inputs.isEmpty()
                    || !ClientStationState.hasMaterials(container, kind, inputs, false)) {
                return missingMaterials();
            }
            if (doTransfer) {
                AddonNetwork.CHANNEL.sendToServer(ServerboundOpenStationPacket.recipeWithIngredients(
                        kind, recipe.getId(), inputs));
            }
            return null;
        }
    }

    private static final class AnvilTransferHandler<C extends AbstractContainerMenu>
            extends StationTransferHandler<C, IJeiAnvilRecipe> {
        @Nullable
        private final StationKind fixedKind;

        private AnvilTransferHandler(Class<? extends C> containerClass,
                                     @Nullable StationKind fixedKind,
                                     IRecipeTransferHandlerHelper helper) {
            super(containerClass, RecipeTypes.ANVIL, helper);
            this.fixedKind = fixedKind;
        }

        @Override
        public IRecipeTransferError transferRecipe(C container, IJeiAnvilRecipe recipe,
                                                   IRecipeSlotsView recipeSlots, Player player,
                                                   boolean maxTransfer, boolean doTransfer) {
            List<ItemStack> left = recipe.getLeftInputs().stream()
                    .filter(stack -> !stack.isEmpty()).limit(64).map(ItemStack::copy).toList();
            List<ItemStack> right = recipe.getRightInputs().stream()
                    .filter(stack -> !stack.isEmpty()).limit(64).map(ItemStack::copy).toList();
            if (left.isEmpty() || right.isEmpty()) {
                return null;
            }
            List<List<ItemStack>> options = List.of(left, right);
            StationKind kind = selectKind();
            if (!ClientStationState.isStationAvailable(kind)) {
                return unavailableStation();
            }
            if (!ClientStationState.hasMaterials(container, kind, options, true)) {
                return missingMaterials();
            }
            if (doTransfer) {
                AddonNetwork.CHANNEL.sendToServer(ServerboundOpenStationPacket.ingredients(
                        kind, options));
            }
            return null;
        }

        private StationKind selectKind() {
            if (fixedKind != null) {
                return fixedKind;
            }
            return ClientStationState.isStationAvailable(StationKind.GOETY_DARK_ANVIL)
                    ? StationKind.GOETY_DARK_ANVIL
                    : StationKind.ANVIL;
        }
    }

    private static final class IronsArcaneAnvilTransferHandler<C extends AbstractContainerMenu>
            extends StationTransferHandler<C, ArcaneAnvilJeiRecipe> {
        private IronsArcaneAnvilTransferHandler(Class<? extends C> containerClass,
                                                IRecipeTransferHandlerHelper helper) {
            super(containerClass, ArcaneAnvilRecipeCategory.ARCANE_ANVIL_RECIPE_RECIPE_TYPE,
                    helper);
        }

        @Override
        public IRecipeTransferError transferRecipe(C container, ArcaneAnvilJeiRecipe recipe,
                                                   IRecipeSlotsView recipeSlots, Player player,
                                                   boolean maxTransfer, boolean doTransfer) {
            StationKind kind = StationKind.IRONS_ARCANE_ANVIL;
            List<List<ItemStack>> inputs = inputOptions(recipeSlots, kind);
            if (!ClientStationState.isStationAvailable(kind)) {
                return unavailableStation();
            }
            if (inputs.isEmpty()
                    || !ClientStationState.hasMaterials(container, kind, inputs, true)) {
                return missingMaterials();
            }
            if (doTransfer) {
                AddonNetwork.CHANNEL.sendToServer(
                        ServerboundOpenStationPacket.ingredients(kind, inputs));
            }
            return null;
        }
    }

    private static final class IronsScrollForgeTransferHandler<C extends AbstractContainerMenu>
            extends StationTransferHandler<C, ScrollForgeRecipe> {
        private IronsScrollForgeTransferHandler(Class<? extends C> containerClass,
                                                IRecipeTransferHandlerHelper helper) {
            super(containerClass, ScrollForgeRecipeCategory.SCROLL_FORGE_RECIPE_RECIPE_TYPE,
                    helper);
        }

        @Override
        public IRecipeTransferError transferRecipe(C container, ScrollForgeRecipe recipe,
                                                   IRecipeSlotsView recipeSlots, Player player,
                                                   boolean maxTransfer, boolean doTransfer) {
            StationKind kind = StationKind.IRONS_SCROLL_FORGE;
            List<List<ItemStack>> inputs = inputOptions(recipeSlots, kind);
            String spellId = scrollForgeSpellId(recipe);
            if (!ClientStationState.isStationAvailable(kind)) {
                return unavailableStation();
            }
            if (spellId == null || inputs.isEmpty()
                    || !ClientStationState.hasMaterials(container, kind, inputs, true)) {
                return missingMaterials();
            }
            if (doTransfer) {
                AddonNetwork.CHANNEL.sendToServer(
                        ServerboundOpenStationPacket.ingredients(kind, inputs));
                AddonNetwork.CHANNEL.sendToServer(
                        new ServerboundSelectIronsScrollSpellPacket(spellId));
            }
            return null;
        }

        @Nullable
        private static String scrollForgeSpellId(ScrollForgeRecipe recipe) {
            for (ItemStack output : recipe.scrollOutputs()) {
                if (output.isEmpty()) {
                    continue;
                }
                SpellData spell = ISpellContainer.get(output).getSpellAtIndex(0);
                if (spell != SpellData.EMPTY) {
                    return spell.getSpell().getSpellId();
                }
            }
            return null;
        }
    }

    private static List<List<ItemStack>> inputOptions(IRecipeSlotsView recipeSlots, StationKind kind) {
        List<IRecipeSlotView> inputSlots = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
        if (!kind.acceptsInputCount(inputSlots.size())) {
            return List.of();
        }
        List<List<ItemStack>> inputs = new java.util.ArrayList<>();
        for (IRecipeSlotView slot : inputSlots) {
            List<ItemStack> options = slot.getItemStacks()
                    .filter(stack -> !stack.isEmpty())
                    .limit(64)
                    .map(ItemStack::copy)
                    .toList();
            if (options.isEmpty()) {
                return List.of();
            }
            inputs.add(options);
        }
        return inputs;
    }
}
