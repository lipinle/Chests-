package com.lipinle.ironchests.block;

import com.lipinle.ironchests.ChestTier;
import com.lipinle.ironchests.menu.MetalChestMenu;
import com.lipinle.ironchests.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MetalChestBlockEntity extends RandomizableContainerBlockEntity implements LidBlockEntity {
    private final ChestTier tier;
    private NonNullList<ItemStack> items;
    private final ChestLidController lidController = new ChestLidController();
    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            playSound(level, pos, SoundEvents.CHEST_OPEN);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            playSound(level, pos, SoundEvents.CHEST_CLOSE);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previousCount, int newCount) {
            level.blockEvent(pos, state.getBlock(), 1, newCount);
        }

        @Override
        protected boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof MetalChestMenu menu && menu.getContainer() == MetalChestBlockEntity.this;
        }
    };

    public MetalChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEST.get(), pos, state);
        this.tier = state.getBlock() instanceof MetalChestBlock block ? block.getTier() : ChestTier.IRON;
        this.items = NonNullList.withSize(tier.getSize(), ItemStack.EMPTY);
    }

    public ChestTier getTier() {
        return tier;
    }

    // ---- lid animation ----

    public static void lidAnimateTick(Level level, BlockPos pos, BlockState state, MetalChestBlockEntity chest) {
        chest.lidController.tickLid();
    }

    @Override
    public float getOpenNess(float partialTicks) {
        return lidController.getOpenness(partialTicks);
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == 1) {
            lidController.shouldBeOpen(type > 0);
            return true;
        }
        return super.triggerEvent(id, type);
    }

    private static void playSound(Level level, BlockPos pos, SoundEvent sound) {
        level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                sound, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
    }

    @Override
    public void startOpen(Player player) {
        if (!remove && !player.isSpectator() && level != null) {
            openersCounter.incrementOpeners(player, level, getBlockPos(), getBlockState());
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!remove && !player.isSpectator() && level != null) {
            openersCounter.decrementOpeners(player, level, getBlockPos(), getBlockState());
        }
    }

    public void recheckOpen() {
        if (!remove && level != null) {
            openersCounter.recheckOpeners(level, getBlockPos(), getBlockState());
        }
    }

    public int getOpenerCount() {
        return openersCounter.getOpenerCount();
    }

    // ---- container ----

    @Override
    public int getContainerSize() {
        return tier.getSize();
    }

    @Override
    public boolean isEmpty() {
        unpackLootTable(null);
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> newItems) {
        this.items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < Math.min(newItems.size(), items.size()); i++) {
            items.set(i, newItems.get(i));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new MetalChestMenu(containerId, inventory, this, tier);
    }

    // ---- save / load ----

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, items);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, items);
        }
    }

    // ---- client sync (crystal chests show their contents) ----

    @Override
    public void setChanged() {
        super.setChanged();
        if (tier.showsContents() && level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return tier.showsContents() ? saveWithoutMetadata() : super.getUpdateTag();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return tier.showsContents() ? ClientboundBlockEntityDataPacket.create(this) : null;
    }

    /** The biggest stacks in the chest, used by the crystal chest renderer. */
    public List<ItemStack> getDisplayedStacks(int max) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        stacks.sort(Comparator.comparingInt(ItemStack::getCount).reversed());
        return stacks.size() > max ? stacks.subList(0, max) : stacks;
    }

    // ---- upgrades ----

    /** Removes and returns all stacks, leaving the chest empty (used when upgrading). */
    public NonNullList<ItemStack> takeAllItems() {
        NonNullList<ItemStack> taken = items;
        this.items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        return taken;
    }

    public void putItems(List<ItemStack> stacks) {
        for (int i = 0; i < Math.min(stacks.size(), items.size()); i++) {
            items.set(i, stacks.get(i));
        }
        setChanged();
    }
}
