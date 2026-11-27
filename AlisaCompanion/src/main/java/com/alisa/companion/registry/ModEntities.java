package com.alisa.companion.registry;

import com.alisa.companion.AlisaCompanion;
import com.alisa.companion.entity.AlisaEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AlisaCompanion.MOD_ID);
    public static final RegistryObject<EntityType<AlisaEntity>> ALISA = ENTITY_TYPES.register("alisa",
        () -> EntityType.Builder.of(AlisaEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.8F).clientTrackingRange(8).build("alisa"));
    private ModEntities() { }
}
