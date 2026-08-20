package com.starfantasy.refinedstorageaddon.compat.disenchanting.client;

import com.starfantasy.refinedstorageaddon.client.ClientGridReturn;
import com.starfantasy.refinedstorageaddon.compat.disenchanting.NetworkDisenchantMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class NetworkDisenchantScreen
        extends AbstractContainerScreen<NetworkDisenchantMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            "disenchanting", "textures/gui/disenchanter.png");
    private static final ResourceLocation DISALLOWED = ResourceLocation.fromNamespaceAndPath(
            "disenchanting", "textures/gui/disenchanter_disallowed.png");
    private boolean returnRequested;

    public NetworkDisenchantScreen(NetworkDisenchantMenu menu, Inventory inventory,
                                   Component title) {
        super(menu, inventory, title);
        imageWidth = 183;
        imageHeight = 154;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        ResourceLocation texture = menu.isBlacklisted() ? DISALLOWED : BACKGROUND;
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.showsCost() && !menu.isBlacklisted()) {
            Component label = Component.translatable("gui.disenchanting.disenchanter.cost")
                    .append(": " + menu.getCost());
            graphics.drawString(font, label, 8, 6, 0x404040, false);
        }
    }

    @Override
    public void onClose() {
        if (!returnRequested) {
            returnRequested = ClientGridReturn.request();
        }
        if (!returnRequested) {
            super.onClose();
        }
    }
}
