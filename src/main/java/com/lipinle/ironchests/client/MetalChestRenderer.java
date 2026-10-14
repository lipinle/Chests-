package com.lipinle.ironchests.client;

import com.lipinle.ironchests.ChestTier;
import com.lipinle.ironchests.block.MetalChestBlock;
import com.lipinle.ironchests.block.MetalChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Draws the chest using the vanilla chest model with our own textures, and for crystal
 * chests also draws the biggest stacks spinning inside.
 */
public class MetalChestRenderer implements BlockEntityRenderer<MetalChestBlockEntity> {
    private static final int MAX_DISPLAYED = 8;
    private static final float[][] ITEM_OFFSETS = {
            {0.3F, 0.15F, 0.3F}, {0.7F, 0.15F, 0.3F}, {0.3F, 0.15F, 0.7F}, {0.7F, 0.15F, 0.7F},
            {0.3F, 0.5F, 0.3F}, {0.7F, 0.5F, 0.3F}, {0.3F, 0.5F, 0.7F}, {0.7F, 0.5F, 0.7F},
    };

    private final ModelPart lid;
    private final ModelPart bottom;
    private final ModelPart lock;
    private final ItemRenderer itemRenderer;

    public MetalChestRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart root = context.bakeLayer(ModelLayers.CHEST);
        this.bottom = root.getChild("bottom");
        this.lid = root.getChild("lid");
        this.lock = root.getChild("lock");
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(MetalChestBlockEntity chest, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Level level = chest.getLevel();
        BlockState state = chest.getBlockState();
        ChestTier tier = chest.getTier();
        Direction facing = level != null && state.hasProperty(MetalChestBlock.FACING)
                ? state.getValue(MetalChestBlock.FACING)
                : Direction.SOUTH;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        float openness = 1.0F - chest.getOpenNess(partialTick);
        openness = 1.0F - openness * openness * openness;

        RenderType renderType = tier.isTransparent()
                ? RenderType.entityCutoutNoCull(tier.getTexture())
                : RenderType.entityCutout(tier.getTexture());
        VertexConsumer consumer = buffer.getBuffer(renderType);

        lid.xRot = -(openness * ((float) Math.PI / 2F));
        lock.xRot = lid.xRot;
        lid.render(poseStack, consumer, packedLight, packedOverlay);
        lock.render(poseStack, consumer, packedLight, packedOverlay);
        bottom.render(poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();

        if (tier.showsContents() && level != null) {
            renderContents(chest, level, partialTick, poseStack, buffer, packedLight, packedOverlay);
        }
    }

    private void renderContents(MetalChestBlockEntity chest, Level level, float partialTick, PoseStack poseStack,
                                MultiBufferSource buffer, int packedLight, int packedOverlay) {
        List<ItemStack> stacks = chest.getDisplayedStacks(MAX_DISPLAYED);
        if (stacks.isEmpty()) {
            return;
        }
        float spin = ((level.getGameTime() % 360L) + partialTick) * 2.0F;
        float scale = stacks.size() == 1 ? 0.6F : 0.35F;
        int seed = (int) chest.getBlockPos().asLong();

        for (int i = 0; i < stacks.size(); i++) {
            float[] offset = stacks.size() == 1 ? new float[]{0.5F, 0.25F, 0.5F} : ITEM_OFFSETS[i];
            poseStack.pushPose();
            poseStack.translate(offset[0], offset[1], offset[2]);
            poseStack.scale(scale, scale, scale);
            poseStack.mulPose(Axis.YP.rotationDegrees(spin + i * 45.0F));
            itemRenderer.renderStatic(stacks.get(i), ItemDisplayContext.GROUND, packedLight, packedOverlay,
                    poseStack, buffer, level, seed + i);
            poseStack.popPose();
        }
    }
}
