package com.starfantasy.refinedstorageaddon.client;

import com.refinedmods.refinedstorage.screen.grid.GridScreen;
import com.starfantasy.refinedstorageaddon.network.AddonNetwork;
import com.starfantasy.refinedstorageaddon.network.ServerboundStationSlotClickPacket;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class StationModuleSlot extends AbstractWidget {
    private final GridScreen screen;
    private final int slotIndex;

    public StationModuleSlot(GridScreen screen, int slotIndex) {
        super(0, 0, 18, 18, Component.translatable(
                "tooltip.star_fantasy_refinedstorage_addon.station_slot.description"));
        this.screen = screen;
        this.slotIndex = slotIndex;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !active || !isMouseOver(mouseX, mouseY) || (button != 0 && button != 1)) {
            return false;
        }
        playDownSound(Minecraft.getInstance().getSoundManager());
        AddonNetwork.CHANNEL.sendToServer(new ServerboundStationSlotClickPacket(
                screen.getMenu().containerId, slotIndex, button));
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ItemStack displayed = ClientStationState.slotItem(slotIndex);
        if (!displayed.isEmpty()) {
            graphics.renderItem(displayed, getX() + 1, getY() + 1);
        }
        if (isHovered) {
            List<Component> tooltip = displayed.isEmpty()
                    ? emptySlotTooltip()
                    : List.of(displayed.getHoverName());
            graphics.renderComponentTooltip(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
        }
    }

    private static List<Component> emptySlotTooltip() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable(
                "tooltip.star_fantasy_refinedstorage_addon.station_slot.description"));
        tooltip.add(Component.translatable(
                "tooltip.star_fantasy_refinedstorage_addon.station_slot.supported"));
        for (StationKind kind : StationKind.values()) {
            if (kind.isInstalled()) {
                tooltip.add(Component.translatable(
                        "tooltip.star_fantasy_refinedstorage_addon.station_slot.entry",
                        kind.activationItemName()));
            }
        }
        return tooltip;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
