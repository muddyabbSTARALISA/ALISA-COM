package com.alisa.companion.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class AlisaEntity extends TamableAnimal {
    public AlisaEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 24.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.30D)
            .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
       this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.12D, 5.0F, 2.0F, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null; // Аля — уникальный компаньон, размножение не предусмотрено.
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide) return InteractionResult.SUCCESS;

        if (this.isOwnedBy(player)) {
            this.setOrderedToSit(!this.isOrderedToSit());
            this.navigation.stop();
            player.displayClientMessage(Component.literal(this.isOrderedToSit()
                ? "Аля присела отдохнуть." : "Аля готова идти с тобой!"), true);
            return InteractionResult.CONSUME;
        }

        if (!this.isTame() && (player.getItemInHand(hand).is(Items.COOKIE)
                || player.getItemInHand(hand).is(Items.SWEET_BERRIES))) {
            if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
            this.tame(player);
            this.setOrderedToSit(false);
            this.navigation.stop();
            this.level().broadcastEntityEvent(this, (byte) 7);
            player.displayClientMessage(Component.literal("Аля доверилась тебе! Теперь она будет твоим компаньоном."), true);
            return InteractionResult.CONSUME;
        }

        if (!this.isTame()) {
            player.displayClientMessage(Component.literal("Аля пока не знает тебя. Попробуй угостить её печеньем или сладкими ягодами."), true);
        }
        return InteractionResult.PASS;
    }
}
