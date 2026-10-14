package com.lipinle.ironchests;

import com.lipinle.ironchests.registry.ModBlockEntities;
import com.lipinle.ironchests.registry.ModBlocks;
import com.lipinle.ironchests.registry.ModCreativeTabs;
import com.lipinle.ironchests.registry.ModItems;
import com.lipinle.ironchests.registry.ModMenus;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(IronChests.MOD_ID)
public class IronChests {
    public static final String MOD_ID = "ironchests";

    public IronChests() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modBus);
        ModMenus.MENU_TYPES.register(modBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modBus);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
