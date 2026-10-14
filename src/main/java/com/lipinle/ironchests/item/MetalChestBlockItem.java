package com.lipinle.ironchests.item;

import com.lipinle.ironchests.block.MetalChestBlock;
import com.lipinle.ironchests.client.MetalChestItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class MetalChestBlockItem extends BlockItem {
    public MetalChestBlockItem(MetalChestBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return MetalChestItemRenderer.getInstance();
            }
        });
    }
}
