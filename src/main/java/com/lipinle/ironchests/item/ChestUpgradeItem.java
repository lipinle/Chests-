package com.lipinle.ironchests.item;

import com.lipinle.ironchests.ChestTier;
import com.lipinle.ironchests.ChestUpgrade;
import com.lipinle.ironchests.block.MetalChestBlock;
import com.lipinle.ironchests.block.MetalChestBlockEntity;
import com.lipinle.ironchests.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Sneak + right click a chest to upgrade it in place, keeping its contents and name.
 */
public class ChestUpgradeItem extends Item {
    private final ChestUpgrade upgrade;

    public ChestUpgradeItem(ChestUpgrade upgrade, Properties properties) {
        super(properties);
        this.upgrade = upgrade;
    }

    public ChestUpgrade getUpgrade() {
        return upgrade;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!canUpgrade(state, blockEntity)) {
            return InteractionResult.PASS;
        }

        if (isInUse(level, pos, blockEntity)) {
            player.displayClientMessage(Component.translatable("message.ironchests.upgrade.in_use").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            player.displayClientMessage(Component.translatable("message.ironchests.upgrade.double_chest").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // Pull everything out of the old chest first so breaking it drops nothing.
        NonNullList<ItemStack> contents;
        Component customName = null;
        if (blockEntity instanceof MetalChestBlockEntity metalChest) {
            metalChest.unpackLootTable(player);
            contents = metalChest.takeAllItems();
            if (metalChest.hasCustomName()) {
                customName = metalChest.getCustomName();
            }
        } else {
            ChestBlockEntity woodChest = (ChestBlockEntity) blockEntity;
            woodChest.unpackLootTable(player);
            contents = NonNullList.withSize(woodChest.getContainerSize(), ItemStack.EMPTY);
            for (int i = 0; i < woodChest.getContainerSize(); i++) {
                contents.set(i, woodChest.removeItemNoUpdate(i));
            }
            if (woodChest.hasCustomName()) {
                customName = woodChest.getCustomName();
            }
        }

        MetalChestBlock target = ModBlocks.get(upgrade.getTo());
        BlockState newState = target.defaultBlockState()
                .setValue(MetalChestBlock.FACING, state.getValue(ChestBlock.FACING))
                .setValue(MetalChestBlock.WATERLOGGED, state.getValue(ChestBlock.WATERLOGGED));
        level.setBlock(pos, newState, Block.UPDATE_ALL);

        if (level.getBlockEntity(pos) instanceof MetalChestBlockEntity newChest) {
            newChest.putItems(contents);
            if (customName != null) {
                newChest.setCustomName(customName);
            }
        }

        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (!player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    private boolean canUpgrade(BlockState state, @Nullable BlockEntity blockEntity) {
        ChestTier from = upgrade.getFrom();
        if (from == null) {
            // Vanilla wooden chests (plain and trapped share ChestBlockEntity).
            return state.getBlock() instanceof ChestBlock && blockEntity instanceof ChestBlockEntity;
        }
        return state.getBlock() instanceof MetalChestBlock block && block.getTier() == from
                && blockEntity instanceof MetalChestBlockEntity;
    }

    private static boolean isInUse(Level level, BlockPos pos, @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof MetalChestBlockEntity metalChest) {
            return metalChest.getOpenerCount() > 0;
        }
        return ChestBlockEntity.getOpenCount(level, pos) > 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Component from = upgrade.getFrom() == null
                ? Component.translatable("block.minecraft.chest")
                : Component.translatable("block.ironchests." + upgrade.getFrom().getBlockName());
        Component to = Component.translatable("block.ironchests." + upgrade.getTo().getBlockName());
        tooltip.add(Component.translatable("tooltip.ironchests.upgrade.from_to", from, to).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.ironchests.upgrade.how").withStyle(ChatFormatting.DARK_GRAY));
    }
}
