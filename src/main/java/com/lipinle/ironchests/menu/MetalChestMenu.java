package com.lipinle.ironchests.menu;

import com.lipinle.ironchests.ChestTier;
import com.lipinle.ironchests.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MetalChestMenu extends AbstractContainerMenu {
    private static final int SLOT = 18;

    private final Container container;
    private final ChestTier tier;

    /** Client side constructor: the tier comes from the open-screen packet. */
    public static MetalChestMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        ChestTier tier = ChestTier.byId(buf.readByte());
        return new MetalChestMenu(containerId, playerInventory, new SimpleContainer(tier.getSize()), tier);
    }

    public MetalChestMenu(int containerId, Inventory playerInventory, Container container, ChestTier tier) {
        super(ModMenus.CHEST.get(), containerId);
        checkContainerSize(container, tier.getSize());
        this.container = container;
        this.tier = tier;
        container.startOpen(playerInventory.player);

        int chestX = tier.getChestSlotsX();
        int chestY = tier.getChestSlotsY();
        for (int row = 0; row < tier.getRows(); row++) {
            for (int col = 0; col < tier.getColumns(); col++) {
                addSlot(new Slot(container, col + row * tier.getColumns(), chestX + col * SLOT, chestY + row * SLOT));
            }
        }

        int invX = tier.getPlayerInvX();
        int invY = tier.getPlayerInvY();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, invX + col * SLOT, invY + row * SLOT));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, invX + col * SLOT, invY + 58));
        }
    }

    public Container getContainer() {
        return container;
    }

    public ChestTier getTier() {
        return tier;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int chestSize = tier.getSize();
            if (index < chestSize) {
                if (!moveItemStackTo(stack, chestSize, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, chestSize, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
