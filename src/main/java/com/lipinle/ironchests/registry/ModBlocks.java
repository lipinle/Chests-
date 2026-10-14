package com.lipinle.ironchests.registry;

import com.lipinle.ironchests.ChestTier;
import com.lipinle.ironchests.IronChests;
import com.lipinle.ironchests.block.MetalChestBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, IronChests.MOD_ID);

    public static final Map<ChestTier, RegistryObject<MetalChestBlock>> CHESTS;

    static {
        Map<ChestTier, RegistryObject<MetalChestBlock>> chests = new EnumMap<>(ChestTier.class);
        for (ChestTier tier : ChestTier.values()) {
            chests.put(tier, BLOCKS.register(tier.getBlockName(), () -> new MetalChestBlock(tier, properties(tier))));
        }
        CHESTS = Collections.unmodifiableMap(chests);
    }

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties properties(ChestTier tier) {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                .mapColor(tier.getMapColor())
                .strength(tier.getHardness(), tier.getResistance())
                .sound(tier.getSoundType());
        if (tier.isTransparent()) {
            props = props.noOcclusion();
        }
        return props;
    }

    public static MetalChestBlock get(ChestTier tier) {
        return CHESTS.get(tier).get();
    }

    public static Block[] all() {
        return CHESTS.values().stream().map(RegistryObject::get).toArray(Block[]::new);
    }
}
