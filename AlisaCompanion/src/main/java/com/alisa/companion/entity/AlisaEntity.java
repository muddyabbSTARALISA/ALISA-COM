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
    private static final int MAX_RELATIONSHIP_HALF_POINTS = 198; // 99 points
    private static final int MAX_GIFTS_PER_DAY = 4;

    // Stored in half-points so the smallest possible increase is exactly 0.5.
    private int relationshipHalfPoints;
    private long giftDay = Long.MIN_VALUE;
    private int giftsToday;

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

    public int getRelationshipHalfPoints() {
        return this.relationshipHalfPoints;
    }

    public String getRelationshipRank() {
        int points = this.relationshipHalfPoints / 2;
        if (points >= 90) return "Партнёр";
        if (points >= 75) return "Лпэшка";
        if (points >= 50) return "Друг";
        if (points >= 25) return "Помощник";
        if (points >= 10) return "Знакомый";
        return "Незнакомец";
    }

    private static String formatHalfPoints(int halfPoints) {
        return (halfPoints % 2 == 0)
            ? Integer.toString(halfPoints / 2)
            : (halfPoints / 2) + ".5";
    }

    private boolean isGift(ItemStack stack) {
        return stack.is(ModItems.CHOCOLATE.get())
            || stack.is(ModItems.SODA.get())
            || stack.is(Items.COOKIE)
            || stack.is(Items.CAKE)
            || stack.is(Items.APPLE)
            || stack.is(Items.SWEET_BERRIES)
            || stack.is(Items.BREAD);
    }

    private boolean isFavoriteGift(ItemStack stack) {
        return stack.is(ModItems.CHOCOLATE.get()) || stack.is(ModItems.SODA.get());
    }

    private InteractionResult tryAcceptGift(Player player, InteractionHand hand) {
        ItemStack offered = player.getItemInHand(hand);
        if (!this.isGift(offered)) return InteractionResult.PASS;

        long currentDay = Math.floorDiv(this.level().getDayTime(), 24000L);
        if (this.giftDay != currentDay) {
            this.giftDay = currentDay;
            this.giftsToday = 0;
        }

        if (this.giftsToday >= MAX_GIFTS_PER_DAY) {
            player.displayClientMessage(Component.literal("Аля уже получила достаточно подарков на сегодня. Попробуй завтра!"), true);
            return InteractionResult.CONSUME;
        }

        int reward = this.isFavoriteGift(offered) ? 2 : 1;
        this.relationshipHalfPoints = Math.min(MAX_RELATIONSHIP_HALF_POINTS,
            this.relationshipHalfPoints + reward);
        this.giftsToday++;

        if (!player.getAbilities().instabuild) offered.shrink(1);

        String reaction = this.isFavoriteGift(offered)
            ? "Але очень понравилось угощение!"
            : "Аля благодарит за подарок.";
        player.displayClientMessage(Component.literal(reaction + " Отношения: "
            + formatHalfPoints(this.relationshipHalfPoints) + "/99 — " + this.getRelationshipRank()), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide) return InteractionResult.SUCCESS;

        if (this.isOwnedBy(player)) {
            InteractionResult giftResult = this.tryAcceptGift(player, hand);
            if (giftResult.consumesAction()) return giftResult;

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

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AlisaRelationshipHalfPoints", this.relationshipHalfPoints);
        tag.putLong("AlisaGiftDay", this.giftDay);
        tag.putInt("AlisaGiftsToday", this.giftsToday);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.relationshipHalfPoints = Math.max(0,
            Math.min(MAX_RELATIONSHIP_HALF_POINTS, tag.getInt("AlisaRelationshipHalfPoints")));
        this.giftDay = tag.contains("AlisaGiftDay") ? tag.getLong("AlisaGiftDay") : Long.MIN_VALUE;
        this.giftsToday = Math.max(0, Math.min(MAX_GIFTS_PER_DAY, tag.getInt("AlisaGiftsToday")));
    }
}
