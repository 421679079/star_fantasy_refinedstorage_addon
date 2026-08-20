package com.starfantasy.refinedstorageaddon.client;

import com.refinedmods.refinedstorage.screen.BaseScreen;
import com.refinedmods.refinedstorage.screen.widget.sidebutton.SideButton;
import com.starfantasy.refinedstorageaddon.network.AddonNetwork;
import com.starfantasy.refinedstorageaddon.network.ServerboundOpenStationPacket;
import com.starfantasy.refinedstorageaddon.station.StationKind;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;

public final class StationOpenButton extends SideButton {
    private final int slotIndex;

    public StationOpenButton(BaseScreen<?> screen, int slotIndex) {
        super(screen);
        this.slotIndex = slotIndex;
    }

    @Override
    public void onPress() {
        StationKind kind = ClientStationState.stationKind(slotIndex);
        if (kind != null) {
            ClientStationState.captureCurrentNetworkItems();
            AddonNetwork.CHANNEL.sendToServer(ServerboundOpenStationPacket.openOnly(kind));
        }
    }

    @Override
    protected void renderButtonIcon(GuiGraphics graphics, int x, int y) {
        graphics.blit(BaseScreen.ICONS_TEXTURE, x, y, 16, 144, 16, 16);
    }

    @Override
    protected String getSideButtonTooltip() {
        ItemStack stack = ClientStationState.slotItem(slotIndex);
        if (stack.isEmpty()) {
            return I18n.get("tooltip.star_fantasy_refinedstorage_addon.station_slot.description");
        }
        return I18n.get("tooltip.star_fantasy_refinedstorage_addon.open",
                stack.getHoverName().getString());
    }
}
