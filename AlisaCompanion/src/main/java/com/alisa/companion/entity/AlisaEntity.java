package com.alisa.companion.entity;

import com.alisa.companion.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
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
    private static final String RELATIONSHIP_TAG = "AlisaRelationshipHalfPoints";
    private static final String LAST_GIFT_TAG = "AlisaLastGiftTime";
    private static final long GIFT_COOLDOWN_TICKS = 6000L;

    // Храним половинки очка целым числом: дробные значения всегда кратны 0,5.
    private int relationshipHalfPoints = 0;
    private long lastGiftGameTime = Long.MIN_VALUE;

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
        return null;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    public double getRelationshipPoints() {
        return relationshipHalfPoints / 2.0D;
    }

    public String getRelationshipStatus() {
        double points = getRelationshipPoints();
        if (points >= 90.0D) return "Партнёр";
        if (points >= 75.0D) return "Лпэшка";
        if (points >= 50.0D) return "Друг";
        if (points >= 25.0D) return "Помощник";
        if (points >= 10.0D) return "Знакомый";
        return "Незнакомец";
    }

    private InteractionResult tryAcceptGift(Player player, ItemStack stack) {
        boolean favorite = stack.is(ModItems.CHOCOLATE.get()) || stack.is(ModItems.SODA.get());
        boolean ordinary = stack.is(Items.COOKIE) || stack.is(Items.SWEET_BERRIES);
        if (!favorite && !ordinary) return InteractionResult.PASS;

        long now = this.level().getGameTime();
        if (lastGiftGameTime != Long.MIN_VALUE && now - lastGiftGameTime < GIFT_COOLDOWN_TICKS) {
            long remainingSeconds = (GIFT_COOLDOWN_TICKS - (now - lastGiftGameTime)) / 20L;
            player.displayClientMessage(Component.literal("Аля пока не хочет больше угощений. Попробуй снова через "
                + remainingSeconds + " сек."), true);
            return InteractionResult.CONSUME;
        }

        if (!player.getAbilities().instabuild) stack.shrink(1);
        relationshipHalfPoints = Math.min(198, relationshipHalfPoints + (favorite ? 2 : 1));
        lastGiftGameTime = now;

        String giftName = favorite ? (stack.is(ModItems.CHOCOLATE.get()) ? "шоколад" : "газировку") : "угощение";
        player.displayClientMessage(Component.literal("Але понравилось " + giftName + "! Отношения: "
            + getRelationshipPoints() + "/99 — " + getRelationshipStatus()), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(RELATIONSHIP_TAG, relationshipHalfPoints);
        tag.putLong(LAST_GIFT_TAG, lastGiftGameTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        relationshipHalfPoints = Math.max(0, Math.min(198, tag.getInt(RELATIONSHIP_TAG)));
        lastGiftGameTime = tag.contains(LAST_GIFT_TAG) ? tag.getLong(LAST_GIFT_TAG) : Long.MIN_VALUE;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide) return InteractionResult.SUCCESS;

        ItemStack held = player.getItemInHand(hand);
        if (this.isOwnedBy(player)) {
            InteractionResult giftResult = tryAcceptGift(player, held);
            if (giftResult.consumesAction()) return giftResult;

            this.setOrderedToSit(!this.isOrderedToSit());
            this.navigation.stop();
            player.displayClientMessage(Component.literal(this.isOrderedToSit()
                ? "Аля присела отдохнуть." : "Аля готова идти с тобой!"), true);
            return InteractionResult.CONSUME;
        }

        if (!this.isTame() && (held.is(Items.COOKIE) || held.is(Items.SWEET_BERRIES))) {
            if (!player.getAbilities().instabuild) held.shrink(1);
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