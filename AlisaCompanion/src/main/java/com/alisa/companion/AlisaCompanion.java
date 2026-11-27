package com.alisa.companion;

import com.alisa.companion.registry.ModEntities;
import com.alisa.companion.registry.ModItems;
import com.alisa.companion.network.AlisaNetwork;
import com.alisa.companion.registry.ModMenuTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.common.MinecraftForge;

@Mod(AlisaCompanion.MOD_ID)
public class AlisaCompanion {
    public static final String MOD_ID = "alisacompanion";
    public static ResourceLocation id(String path) { return new ResourceLocation(MOD_ID, path); }

    public AlisaCompanion() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITY_TYPES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModMenuTypes.MENUS.register(modBus);
        AlisaNetwork.register();
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::createAttributes);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) { }

    private void createAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ALISA.get(), com.alisa.companion.entity.AlisaEntity.createAttributes().build());
    }
}
