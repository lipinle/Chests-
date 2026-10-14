package com.lipinle.ironchests.registry;

import com.lipinle.ironchests.IronChests;
import com.lipinle.ironchests.block.MetalChestBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, IronChests.MOD_ID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<MetalChestBlockEntity>> CHEST = BLOCK_ENTITY_TYPES.register("chest",
            () -> BlockEntityType.Builder.of(MetalChestBlockEntity::new, ModBlocks.all()).build(null));

    private ModBlockEntities() {
    }
}
