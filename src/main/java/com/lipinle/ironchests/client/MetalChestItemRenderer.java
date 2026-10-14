package com.lipinle.ironchests.client;

import com.lipinle.ironchests.ChestTier;
import com.lipinle.ironchests.block.MetalChestBlock;
import com.lipinle.ironchests.block.MetalChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/**
 * Renders chest items in hand / inventory with the same 3D model as the placed block.
 */
public class MetalChestItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static MetalChestItemRenderer instance;

    private final Map<ChestTier, MetalChestBlockEntity> dummies = new EnumMap<>(ChestTier.class);

    private MetalChestItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static MetalChestItemRenderer getInstance() {
        if (instance == null) {
            instance = new MetalChestItemRenderer();
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof MetalChestBlock block) {
            MetalChestBlockEntity dummy = dummies.computeIfAbsent(block.getTier(),
                    tier -> new MetalChestBlockEntity(BlockPos.ZERO, block.defaultBlockState()));
            Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(dummy, poseStack, buffer, packedLight, packedOverlay);
        }
    }
}
