package com.lipinle.ironchests.registry;

import com.lipinle.ironchests.IronChests;
import com.lipinle.ironchests.menu.MetalChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, IronChests.MOD_ID);

    public static final RegistryObject<MenuType<MetalChestMenu>> CHEST = MENU_TYPES.register("chest",
            () -> IForgeMenuType.create(MetalChestMenu::fromNetwork));

    private ModMenus() {
    }
}
