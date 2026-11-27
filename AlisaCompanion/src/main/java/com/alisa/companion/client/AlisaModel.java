package com.alisa.companion.client;

import com.alisa.companion.AlisaCompanion;
import com.alisa.companion.entity.AlisaEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Модель Али поверх UV-совместимой базы игрока.
 * Дополнительные детали одежды и волос имеют собственную геометрию.
 * Вторичная физика реализована пружинной моделью на клиенте (без столкновений).
 */
public class AlisaModel extends PlayerModel<AlisaEntity> {
    public static final ModelLayerLocation DETAIL_LAYER = new ModelLayerLocation(
        new ResourceLocation(AlisaCompanion.MOD_ID, "alisa_details"), "main");

    private final ModelPart fringe;
    private final ModelPart leftLock;
    private final ModelPart rightLock;
    private final ModelPart backHair;
    private final ModelPart hood;
    private final ModelPart hoodieBody;
    private final ModelPart hoodieHem;
    private final ModelPart leftSleeve;
    private final ModelPart rightSleeve;
    private final ModelPart leftCuff;
    private final ModelPart rightCuff;

    // Состояние spring-damper: углы и скорости вторичных частей.
    private float hairSwing;
    private float hairVelocity;
    private float hemSwing;
    private float hemVelocity;
    private float previousBodyYaw;
    private boolean physicsInitialized;

    public AlisaModel(EntityRendererProvider.Context context) {
        super(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER), false);
        ModelPart details = context.bakeLayer(DETAIL_LAYER);
        this.fringe = details.getChild("fringe");
        this.leftLock = details.getChild("left_lock");
        this.rightLock = details.getChild("right_lock");
        this.backHair = details.getChild("back_hair");
        this.hood = details.getChild("hood");
        this.hoodieBody = details.getChild("hoodie_body");
        this.hoodieHem = details.getChild("hoodie_hem");
        this.leftSleeve = details.getChild("left_sleeve");
        this.rightSleeve = details.getChild("right_sleeve");
        this.leftCuff = details.getChild("left_cuff");
        this.rightCuff = details.getChild("right_cuff");

        // Компактный силуэт, близкий к исходному персонажу.
        scalePart(this.head, 1.04F, 1.04F, 1.04F);
        scalePart(this.hat, 1.04F, 1.04F, 1.04F);
        scalePart(this.body, 0.96F, 1.0F, 0.96F);
        scalePart(this.jacket, 0.96F, 1.0F, 0.96F);
        scalePart(this.leftArm, 0.94F, 1.0F, 0.94F);
        scalePart(this.rightArm, 0.94F, 1.0F, 0.94F);
        scalePart(this.leftSleeve, 0.94F, 1.0F, 0.94F);
        scalePart(this.rightSleeve, 0.94F, 1.0F, 0.94F);
    }

    public static LayerDefinition createDetailsLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Волосы: несколько отдельных объёмных прядей, UV берутся из чёрной зоны скина.
        root.addOrReplaceChild("fringe", CubeListBuilder.create().texOffs(32, 0)
            .addBox(-4.15F, -7.65F, -4.72F, 8.3F, 3.45F, 1.35F), PartPose.ZERO);
        root.addOrReplaceChild("left_lock", CubeListBuilder.create().texOffs(32, 0)
            .addBox(-4.35F, -4.9F, -4.15F, 1.45F, 6.2F, 1.35F), PartPose.ZERO);
        root.addOrReplaceChild("right_lock", CubeListBuilder.create().texOffs(32, 0)
            .addBox(2.9F, -4.9F, -4.15F, 1.45F, 6.2F, 1.35F), PartPose.ZERO);
        root.addOrReplaceChild("back_hair", CubeListBuilder.create().texOffs(32, 0)
            .addBox(-4.0F, -2.8F, 3.25F, 8.0F, 6.4F, 1.45F), PartPose.ZERO);

        // Капюшон позади головы — отдельная объёмная деталь.
        root.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(32, 0)
            .addBox(-4.55F, -4.1F, 2.55F, 9.1F, 7.2F, 2.0F), PartPose.ZERO);

        // Объёмный внешний слой худи и нижняя кромка. Базовый скин сохраняет шахматный знак.
        root.addOrReplaceChild("hoodie_body", CubeListBuilder.create().texOffs(16, 32)
            .addBox(-4.35F, 0.0F, -2.35F, 8.7F, 12.2F, 4.7F), PartPose.ZERO);
        root.addOrReplaceChild("hoodie_hem", CubeListBuilder.create().texOffs(16, 32)
            .addBox(-4.42F, 9.5F, -2.42F, 8.84F, 2.9F, 4.84F), PartPose.ZERO);

        // Наружные рукава и светлые манжеты повторяют позу базовых рук.
        root.addOrReplaceChild("left_sleeve", CubeListBuilder.create().texOffs(40, 32)
            .addBox(-2.15F, -2.15F, -2.15F, 4.3F, 12.3F, 4.3F), PartPose.ZERO);
        root.addOrReplaceChild("right_sleeve", CubeListBuilder.create().texOffs(40, 32)
            .addBox(-2.15F, -2.15F, -2.15F, 4.3F, 12.3F, 4.3F), PartPose.ZERO);
        root.addOrReplaceChild("left_cuff", CubeListBuilder.create().texOffs(40, 48)
            .addBox(-2.18F, 7.1F, -2.18F, 4.36F, 4.8F, 4.36F), PartPose.ZERO);
        root.addOrReplaceChild("right_cuff", CubeListBuilder.create().texOffs(40, 48)
            .addBox(-2.18F, 7.1F, -2.18F, 4.36F, 4.8F, 4.36F), PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void scalePart(ModelPart part, float x, float y, float z) {
        part.xScale *= x;
        part.yScale *= y;
        part.zScale *= z;
    }

    @Override
    public void setupAnim(AlisaEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        float idle = (float) Math.sin(ageInTicks * 0.075F);
        updateSecondaryMotion(entity, ageInTicks, limbSwingAmount);

        // Детали головы наследуют полный transform головы.
        copyPose(this.head, this.fringe);
        copyPose(this.head, this.leftLock);
        copyPose(this.head, this.rightLock);
        copyPose(this.head, this.backHair);
        copyPose(this.head, this.hood);
        this.fringe.zRot = this.head.zRot;
        this.leftLock.zRot = this.head.zRot + hairSwing + idle * 0.01F;
        this.rightLock.zRot = this.head.zRot - hairSwing - idle * 0.01F;
        this.backHair.zRot = this.head.zRot + hairSwing * 0.35F;
        this.leftLock.xRot += hairSwing * 0.22F;
        this.rightLock.xRot -= hairSwing * 0.22F;
        this.backHair.xRot += hairSwing * 0.12F;

        if (entity.isOrderedToSit()) {
            this.leftLeg.xRot = -1.38F;
            this.rightLeg.xRot = -1.38F;
            this.leftLeg.y = 9.0F;
            this.rightLeg.y = 9.0F;
            this.leftArm.xRot = -0.22F + idle * 0.02F;
            this.rightArm.xRot = -0.22F - idle * 0.02F;
            this.body.xRot = -0.08F;
        } else {
            this.leftArm.zRot += idle * 0.015F;
            this.rightArm.zRot -= idle * 0.015F;
        }

        // Слои одежды копируют уже рассчитанную позу тела и рук.
        copyPose(this.body, this.hoodieBody);
        copyPose(this.body, this.hoodieHem);
        this.hoodieHem.xRot += hemSwing;
        copyPose(this.leftArm, this.leftSleeve);
        copyPose(this.rightArm, this.rightSleeve);
        copyPose(this.leftArm, this.leftCuff);
        copyPose(this.rightArm, this.rightCuff);
    }

    /** Пружинная вторичная анимация от ускорения и поворота тела. */
    private void updateSecondaryMotion(AlisaEntity entity, float ageInTicks, float limbSwingAmount) {
        Vec3 velocity = entity.getDeltaMovement();
        float speed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float bodyYaw = entity.yBodyRot;
        if (!physicsInitialized) {
            previousBodyYaw = bodyYaw;
            physicsInitialized = true;
        }
        float yawDelta = Mth.wrapDegrees(bodyYaw - previousBodyYaw);
        previousBodyYaw = bodyYaw;

        float hairTarget = Mth.clamp(-speed * 1.8F - yawDelta * 0.012F, -0.42F, 0.42F);
        float hemTarget = Mth.clamp(speed * 0.75F + limbSwingAmount * 0.10F, -0.18F, 0.22F);
        hairVelocity += (hairTarget - hairSwing) * 0.24F;
        hairVelocity *= 0.72F;
        hairSwing = Mth.clamp(hairSwing + hairVelocity, -0.48F, 0.48F);
        hemVelocity += (hemTarget - hemSwing) * 0.18F;
        hemVelocity *= 0.68F;
        hemSwing = Mth.clamp(hemSwing + hemVelocity, -0.20F, 0.24F);

        if (entity.isOrderedToSit()) {
            hairSwing *= 0.94F;
            hemSwing *= 0.90F;
        }
    }

    private static void copyPose(ModelPart source, ModelPart target) {
        target.x = source.x;
        target.y = source.y;
        target.z = source.z;
        target.xRot = source.xRot;
        target.yRot = source.yRot;
        target.zRot = source.zRot;
        target.xScale = source.xScale;
        target.yScale = source.yScale;
        target.zScale = source.zScale;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.hood.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.fringe.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftLock.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.rightLock.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.backHair.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.hoodieBody.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.hoodieHem.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftSleeve.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.rightSleeve.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftCuff.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.rightCuff.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
