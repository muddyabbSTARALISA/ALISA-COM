package com.alisa.companion.client;

import com.alisa.companion.AlisaCompanion;
import com.alisa.companion.entity.AlisaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class AlisaRenderer extends MobRenderer<AlisaEntity, AlisaModel> {
    private static final ResourceLocation TEXTURE =
        new ResourceLocation(AlisaCompanion.MOD_ID, "textures/entity/alisa.png");

    public AlisaRenderer(EntityRendererProvider.Context context) {
        super(context, new AlisaModel(context), 0.32F);
    }

    @Override
    public ResourceLocation getTextureLocation(AlisaEntity entity) {
        return TEXTURE;
    }
}
