package com.lipinle.ironchests.client;

import com.lipinle.ironchests.ChestTier;
import com.lipinle.ironchests.menu.MetalChestMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Chest GUI drawn in code (vanilla-style panel and slots), so every chest size
 * gets a matching background without needing a texture per size.
 */
public class MetalChestScreen extends AbstractContainerScreen<MetalChestMenu> {
    private static final int BORDER = 0xFF000000;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int HIGHLIGHT = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF555555;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_FILL = 0xFF8B8B8B;

    public MetalChestScreen(MetalChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        ChestTier tier = menu.getTier();
        this.imageWidth = tier.getGuiWidth();
        this.imageHeight = tier.getGuiHeight();
        this.titleLabelX = Math.min(8, tier.getChestSlotsX());
        this.inventoryLabelX = tier.getPlayerInvX();
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        int w = imageWidth;
        int h = imageHeight;

        // Rounded vanilla-like panel
        graphics.fill(x + 2, y, x + w - 2, y + h, BORDER);
        graphics.fill(x, y + 2, x + w, y + h - 2, BORDER);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, BORDER);
        graphics.fill(x + 1, y + 2, x + w - 1, y + h - 2, PANEL);
        graphics.fill(x + 2, y + 1, x + w - 2, y + h - 1, PANEL);
        graphics.fill(x + 2, y + 1, x + w - 3, y + 3, HIGHLIGHT);
        graphics.fill(x + 1, y + 2, x + 3, y + h - 3, HIGHLIGHT);
        graphics.fill(x + 3, y + h - 3, x + w - 2, y + h - 1, SHADOW);
        graphics.fill(x + w - 3, y + 3, x + w - 1, y + h - 2, SHADOW);

        for (Slot slot : menu.slots) {
            drawSlot(graphics, x + slot.x - 1, y + slot.y - 1);
        }
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, SLOT_FILL);
        graphics.fill(x, y, x + 17, y + 1, SLOT_DARK);
        graphics.fill(x, y, x + 1, y + 17, SLOT_DARK);
        graphics.fill(x + 1, y + 17, x + 18, y + 18, HIGHLIGHT);
        graphics.fill(x + 17, y + 1, x + 18, y + 18, HIGHLIGHT);
    }
}
