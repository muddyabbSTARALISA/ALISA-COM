package com.alisa.companion.registry;

import com.alisa.companion.AlisaCompanion;
import com.alisa.companion.menu.AlisaInventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, AlisaCompanion.MOD_ID);

    public static final RegistryObject<MenuType<AlisaInventoryMenu>> ALISA_INVENTORY =
        MENUS.register("alisa_inventory", () -> IForgeMenuType.create(AlisaInventoryMenu::new));

    private ModMenuTypes() { }
}
