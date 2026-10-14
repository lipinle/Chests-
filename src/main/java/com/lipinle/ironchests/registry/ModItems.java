package com.lipinle.ironchests.registry;

import com.lipinle.ironchests.ChestTier;
import com.lipinle.ironchests.ChestUpgrade;
import com.lipinle.ironchests.IronChests;
import com.lipinle.ironchests.item.ChestUpgradeItem;
import com.lipinle.ironchests.item.MetalChestBlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, IronChests.MOD_ID);

    public static final Map<ChestTier, RegistryObject<MetalChestBlockItem>> CHESTS;
    public static final Map<ChestUpgrade, RegistryObject<ChestUpgradeItem>> UPGRADES;

    static {
        Map<ChestTier, RegistryObject<MetalChestBlockItem>> chests = new EnumMap<>(ChestTier.class);
        for (ChestTier tier : ChestTier.values()) {
            chests.put(tier, ITEMS.register(tier.getBlockName(),
                    () -> new MetalChestBlockItem(ModBlocks.get(tier), new Item.Properties())));
        }
        CHESTS = Collections.unmodifiableMap(chests);

        Map<ChestUpgrade, RegistryObject<ChestUpgradeItem>> upgrades = new EnumMap<>(ChestUpgrade.class);
        for (ChestUpgrade upgrade : ChestUpgrade.values()) {
            upgrades.put(upgrade, ITEMS.register(upgrade.getItemName(),
                    () -> new ChestUpgradeItem(upgrade, new Item.Properties().stacksTo(16))));
        }
        UPGRADES = Collections.unmodifiableMap(upgrades);
    }

    private ModItems() {
    }
}
