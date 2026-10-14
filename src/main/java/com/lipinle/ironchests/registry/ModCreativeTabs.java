package com.lipinle.ironchests.registry;

import com.lipinle.ironchests.ChestTier;
import com.lipinle.ironchests.IronChests;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IronChests.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + IronChests.MOD_ID))
            .icon(() -> new ItemStack(ModItems.CHESTS.get(ChestTier.IRON).get()))
            .displayItems((params, output) -> {
                ModItems.CHESTS.values().forEach(item -> output.accept(item.get()));
                ModItems.UPGRADES.values().forEach(item -> output.accept(item.get()));
            })
            .build());

    private ModCreativeTabs() {
    }
}
