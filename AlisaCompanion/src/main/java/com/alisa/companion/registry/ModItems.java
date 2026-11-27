package com.alisa.companion.registry;

import com.alisa.companion.AlisaCompanion;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AlisaCompanion.MOD_ID);
    public static final RegistryObject<Item> ALISA_SPAWN_EGG = ITEMS.register("alisa_spawn_egg",
        () -> new ForgeSpawnEggItem(ModEntities.ALISA, 0x171719, 0x42A69B, new Item.Properties()));
    private ModItems() { }
}
