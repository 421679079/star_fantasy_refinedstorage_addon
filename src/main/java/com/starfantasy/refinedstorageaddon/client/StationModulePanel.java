package com.starfantasy.refinedstorageaddon.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Refined Storage's own four-slot grid panel, extended by repeating its slot row. */
public final class StationModulePanel extends AbstractWidget {
    private static final ResourceLocation GRID_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "refinedstorage", "textures/gui/grid.png");
    private static final int PANEL_WIDTH = 30;
    private static final int TOP_HEIGHT = 5;
    private static final int ROW_HEIGHT = 18;
    private static final int BOTTOM_HEIGHT = 5;

    public StationModulePanel(int x, int y, int slotCount) {
        super(x, y, PANEL_WIDTH,
                TOP_HEIGHT + slotCount * ROW_HEIGHT + BOTTOM_HEIGHT, Component.empty());
        active = false;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(GRID_TEXTURE, getX(), getY(), 197, 0,
                PANEL_WIDTH, TOP_HEIGHT);
        int rows = (height - TOP_HEIGHT - BOTTOM_HEIGHT) / ROW_HEIGHT;
        for (int index = 0; index < rows; index++) {
            graphics.blit(GRID_TEXTURE, getX(), getY() + TOP_HEIGHT + index * ROW_HEIGHT,
                    197, 5, PANEL_WIDTH, ROW_HEIGHT);
        }
        graphics.blit(GRID_TEXTURE, getX(), getY() + height - BOTTOM_HEIGHT,
                197, 77, PANEL_WIDTH, BOTTOM_HEIGHT);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
