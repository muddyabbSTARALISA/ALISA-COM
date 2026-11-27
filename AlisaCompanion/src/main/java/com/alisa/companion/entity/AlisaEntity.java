package com.alisa.companion.entity;

import com.alisa.companion.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class AlisaEntity extends TamableAnimal {
    private static final String RELATIONSHIP_TAG = "AlisaRelationshipHalfPoints";
    private static final String LAST_GIFT_TAG = "AlisaLastGiftTime";
    private static final long GIFT_COOLDOWN_TICKS = 600L; // 30 секунд, чтобы отношения можно было проверять без долгого ожидания.

    private static final String QUEST_TYPE_TAG = "AlisaQuestType";
    private static final String QUEST_ITEM_TAG = "AlisaQuestItem";
    private static final String QUEST_PROGRESS_TAG = "AlisaQuestProgress";
    private static final String QUEST_FOODS_TAG = "AlisaQuestFoods";
    private static final String LAST_FOOD_QUEST_TAG = "AlisaLastFoodQuest";
    private static final String LAST_ARMOR_QUEST_TAG = "AlisaLastArmorQuest";
    private static final String LAST_DECOR_QUEST_TAG = "AlisaLastDecorQuest";
    private static final String LAST_QUEST_OFFER_TAG = "AlisaLastQuestOffer";

    private static final int QUEST_NONE = 0;
    private static final int QUEST_FOOD = 1;
    private static final int QUEST_ARMOR = 2;
    private static final int QUEST_DECOR = 3;
    private static final int FOOD_TYPES_REQUIRED = 3;
    private static final long QUEST_TYPE_COOLDOWN_TICKS = 1200L; // 1 минута между заданиями одного типа.
    private static final long AUTO_QUEST_OFFER_TICKS = 12000L;

    // Очки хранятся половинками, чтобы значения всегда были кратны 0,5.
    private int relationshipHalfPoints = 0;
    private long lastGiftGameTime = Long.MIN_VALUE;

    // Активное задание и защита от повторного получения награды.
    private int questType = QUEST_NONE;
    private String questItemId = "";
    private int questProgress = 0;
    private final List<String> questFoods = new ArrayList<>();
    private long lastFoodQuestTime = Long.MIN_VALUE;
    private long lastArmorQuestTime = Long.MIN_VALUE;
    private long lastDecorQuestTime = Long.MIN_VALUE;
    private long lastQuestOfferTime = Long.MIN_VALUE;
    private boolean stayHere;

    public AlisaEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 24.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.23D)
            .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 0.90D, 5.0F, 2.0F, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.65D));
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

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || !this.isTame() || this.questType != QUEST_NONE) return;

        Entity owner = this.getOwner();
        if (!(owner instanceof Player player) || this.distanceToSqr(player) > 64.0D) return;

        long now = this.level().getGameTime();
        if (lastQuestOfferTime == Long.MIN_VALUE) {
            lastQuestOfferTime = now;
            return;
        }
        if (now - lastQuestOfferTime >= AUTO_QUEST_OFFER_TICKS) {
            lastQuestOfferTime = now;
            startQuest(player, false);
        }
    }

    public void sendRelationshipStatus(Player player) {
        player.displayClientMessage(Component.literal("Отношения с Алей: " + getRelationshipPoints()
            + "/99 — " + getRelationshipStatus()), false);
    }

    public void requestQuestFromMenu(Player player) {
        if (questType == QUEST_NONE) startQuest(player, true);
        else showQuestStatus(player);
    }

    public void sendDialogGreeting(Player player) {
        player.displayClientMessage(Component.literal("Аля: «Я здесь. Что будем делать?»"), false);
    }

    public void toggleStayHere(Player player) {
        stayHere = !stayHere;
        this.setOrderedToSit(stayHere);
        this.navigation.stop();
        player.displayClientMessage(Component.literal(stayHere
            ? "Аля останется здесь." : "Аля снова будет следовать за тобой."), false);
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

    private void addRelationshipHalfPoints(int amount) {
        relationshipHalfPoints = Math.max(0, Math.min(198, relationshipHalfPoints + amount));
    }

    private boolean isQuestTypeReady(long lastTime, long now) {
        return lastTime == Long.MIN_VALUE || now - lastTime >= QUEST_TYPE_COOLDOWN_TICKS;
    }

    private boolean isQuestFood(ItemStack stack) {
        return stack.isEdible() || stack.is(ModItems.CHOCOLATE.get()) || stack.is(ModItems.SODA.get());
    }

    private boolean isPreferredDecorationBlock(Block block) {
        return block == Blocks.OBSIDIAN
            || block == Blocks.AMETHYST_BLOCK
            || block == Blocks.QUARTZ_BLOCK
            || block == Blocks.SEA_LANTERN
            || block == Blocks.GLOWSTONE
            || block == Blocks.POLISHED_BLACKSTONE
            || block == Blocks.BLACK_CONCRETE
            || block == Blocks.WHITE_CONCRETE
            || block == Blocks.PINK_CONCRETE
            || block == Blocks.BLACK_WOOL
            || block == Blocks.WHITE_WOOL
            || block == Blocks.PINK_WOOL;
    }

    private List<Block> getAvailableDecorationBlocks(Player player) {
        List<Block> available = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                if (isPreferredDecorationBlock(block) && !available.contains(block)) {
                    available.add(block);
                }
            }
        }
        return available;
    }

    private void startQuest(Player player, boolean requested) {
        long now = this.level().getGameTime();
        List<Integer> available = new ArrayList<>();
        if (isQuestTypeReady(lastFoodQuestTime, now)) available.add(QUEST_FOOD);
        if (isQuestTypeReady(lastArmorQuestTime, now)) available.add(QUEST_ARMOR);
        if (isQuestTypeReady(lastDecorQuestTime, now) && !getAvailableDecorationBlocks(player).isEmpty()) {
            available.add(QUEST_DECOR);
        }

        if (available.isEmpty()) {
            player.displayClientMessage(Component.literal("Аля пока не придумала нового задания. Попробуй обратиться к ней позже."), false);
            return;
        }

        int selected = available.get(this.random.nextInt(available.size()));
        questType = selected;
        questProgress = 0;
        questFoods.clear();
        questItemId = "";

        if (selected == QUEST_FOOD) {
            lastFoodQuestTime = now;
            player.displayClientMessage(Component.literal("Аля: «Принеси мне три разных вида еды! Шоколад и газировка мне особенно нравятся»."), false);
        } else if (selected == QUEST_ARMOR) {
            lastArmorQuestTime = now;
            player.displayClientMessage(Component.literal("Аля: «Принеси мне любую часть брони! Чёрная или белая будет особенно красивой, розовая тоже подойдёт. Просто возьми броню в руку и нажми ПКМ по мне»."), false);
        } else {
            List<Block> blocks = getAvailableDecorationBlocks(player);
            Block requestedBlock = blocks.get(this.random.nextInt(blocks.size()));
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(requestedBlock);
            questItemId = id == null ? "" : id.toString();
            lastDecorQuestTime = now;
            player.displayClientMessage(Component.literal("Аля: «Поставь рядом со мной красивый блок для украшения нашего места: "
                + requestedBlock.getName().getString() + "»."), false);
        }

        lastQuestOfferTime = now;
        if (requested) {
            player.displayClientMessage(Component.literal("Задание принято. Выполни просьбу Алисы, чтобы получить очки отношений."), false);
        }
    }

    private void showQuestStatus(Player player) {
        if (questType == QUEST_NONE) {
            player.displayClientMessage(Component.literal("Активного задания нет. Попроси Алю о задании ещё раз."), true);
        } else if (questType == QUEST_FOOD) {
            player.displayClientMessage(Component.literal("Задание Алисы: принести три разных вида еды. Прогресс: "
                + questFoods.size() + "/" + FOOD_TYPES_REQUIRED + "."), true);
        } else if (questType == QUEST_ARMOR) {
            player.displayClientMessage(Component.literal("Задание Алисы: принеси любую часть брони. Возьми её в руку и нажми ПКМ по Але. Прогресс: "
                + questProgress + "/1."), true);
        } else if (questType == QUEST_DECOR) {
            player.displayClientMessage(Component.literal("Задание Алисы: поставить рядом блок "
                + questItemId + ". Подойди к Але и поставь его не дальше шести блоков."), true);
        }
    }

    private void completeQuest(Player player, int rewardHalfPoints, String message) {
        addRelationshipHalfPoints(rewardHalfPoints);
        player.displayClientMessage(Component.literal(message + " Отношения: "
            + getRelationshipPoints() + "/99 — " + getRelationshipStatus()), false);
        questType = QUEST_NONE;
        questItemId = "";
        questProgress = 0;
        questFoods.clear();
    }

    private InteractionResult tryProgressQuest(Player player, ItemStack stack) {
        if (questType == QUEST_FOOD && isQuestFood(stack)) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            String key = itemId == null ? stack.getItem().toString() : itemId.toString();
            if (questFoods.contains(key)) {
                player.displayClientMessage(Component.literal("Аля: «Этот вид еды ты уже приносил. Попробуй что-нибудь другое!»"), true);
                return InteractionResult.CONSUME;
            }

            questFoods.add(key);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.displayClientMessage(Component.literal("Аля обрадовалась угощению! Разных видов еды: "
                + questFoods.size() + "/" + FOOD_TYPES_REQUIRED + "."), true);
            if (questFoods.size() >= FOOD_TYPES_REQUIRED) {
                completeQuest(player, 2, "Задание выполнено! Аля рада разнообразному угощению. Награда: +1 очко.");
            }
            return InteractionResult.CONSUME;
        }

        if (questType == QUEST_ARMOR && stack.getItem() instanceof ArmorItem) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            completeQuest(player, 3, "Задание выполнено! Аля получила новую броню. Награда: +1,5 очка.");
            return InteractionResult.CONSUME;
        }
        if (questType != QUEST_NONE && !stack.isEmpty()) {
            String hint = questType == QUEST_FOOD
                ? "Аля просит еду. Передай ей три разных вида еды по одному."
                : questType == QUEST_ARMOR
                    ? "Аля просит броню. Возьми любую часть брони в руку и нажми ПКМ по ней."
                    : "Аля просит поставить указанный декоративный блок рядом с ней.";
            player.displayClientMessage(Component.literal(hint), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    public boolean hasActiveDecorationQuest() {
        return questType == QUEST_DECOR && !questItemId.isEmpty();
    }

    public boolean isRequestedDecorationBlock(Block block) {
        if (!hasActiveDecorationQuest()) return false;
        ResourceLocation requestedId = ResourceLocation.tryParse(questItemId);
        return requestedId != null && block == ForgeRegistries.BLOCKS.getValue(requestedId);
    }

    public void completeDecorationQuest(Player player) {
        if (this.level().isClientSide || !hasActiveDecorationQuest()) return;
        completeQuest(player, 4, "Задание выполнено! Аля довольна украшением. Награда: +2 очка.");
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

        String giftName = stack.is(ModItems.CHOCOLATE.get()) ? "шоколад"
            : stack.is(ModItems.SODA.get()) ? "газировку" : "угощение";
        if (!player.getAbilities().instabuild) stack.shrink(1);
        addRelationshipHalfPoints(favorite ? 2 : 1);
        lastGiftGameTime = now;
        player.displayClientMessage(Component.literal("Але понравилось " + giftName + "! Отношения: "
            + getRelationshipPoints() + "/99 — " + getRelationshipStatus()), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(RELATIONSHIP_TAG, relationshipHalfPoints);
        tag.putLong(LAST_GIFT_TAG, lastGiftGameTime);
        tag.putInt(QUEST_TYPE_TAG, questType);
        tag.putString(QUEST_ITEM_TAG, questItemId);
        tag.putInt(QUEST_PROGRESS_TAG, questProgress);

        ListTag foods = new ListTag();
        for (String food : questFoods) foods.add(StringTag.valueOf(food));
        tag.put(QUEST_FOODS_TAG, foods);

        tag.putLong(LAST_FOOD_QUEST_TAG, lastFoodQuestTime);
        tag.putLong(LAST_ARMOR_QUEST_TAG, lastArmorQuestTime);
        tag.putLong(LAST_DECOR_QUEST_TAG, lastDecorQuestTime);
        tag.putLong(LAST_QUEST_OFFER_TAG, lastQuestOfferTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        relationshipHalfPoints = Math.max(0, Math.min(198, tag.getInt(RELATIONSHIP_TAG)));
        lastGiftGameTime = tag.contains(LAST_GIFT_TAG) ? tag.getLong(LAST_GIFT_TAG) : Long.MIN_VALUE;

        questType = Math.max(QUEST_NONE, Math.min(QUEST_DECOR, tag.getInt(QUEST_TYPE_TAG)));
        questItemId = tag.getString(QUEST_ITEM_TAG);
        questProgress = Math.max(0, tag.getInt(QUEST_PROGRESS_TAG));
        questFoods.clear();
        ListTag foods = tag.getList(QUEST_FOODS_TAG, Tag.TAG_STRING);
        for (int i = 0; i < foods.size(); i++) questFoods.add(foods.getString(i));

        lastFoodQuestTime = tag.contains(LAST_FOOD_QUEST_TAG) ? tag.getLong(LAST_FOOD_QUEST_TAG) : Long.MIN_VALUE;
        lastArmorQuestTime = tag.contains(LAST_ARMOR_QUEST_TAG) ? tag.getLong(LAST_ARMOR_QUEST_TAG) : Long.MIN_VALUE;
        lastDecorQuestTime = tag.contains(LAST_DECOR_QUEST_TAG) ? tag.getLong(LAST_DECOR_QUEST_TAG) : Long.MIN_VALUE;
        lastQuestOfferTime = tag.contains(LAST_QUEST_OFFER_TAG) ? tag.getLong(LAST_QUEST_OFFER_TAG) : Long.MIN_VALUE;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide) return InteractionResult.SUCCESS;

        ItemStack held = player.getItemInHand(hand);
        if (this.isOwnedBy(player)) {
            if (player.isShiftKeyDown() && held.isEmpty()) {
                player.displayClientMessage(Component.literal("Отношения с Алей: "
                    + getRelationshipPoints() + "/99 — " + getRelationshipStatus()), true);
                if (questType == QUEST_NONE) startQuest(player, true);
                else showQuestStatus(player);
                return InteractionResult.CONSUME;
            }

            InteractionResult questResult = tryProgressQuest(player, held);
            if (questResult.consumesAction()) return questResult;

            InteractionResult giftResult = tryAcceptGift(player, held);
            if (giftResult.consumesAction()) return giftResult;

            return InteractionResult.PASS;
        }

        if (!this.isTame() && (held.is(Items.COOKIE) || held.is(Items.SWEET_BERRIES)
            || held.is(ModItems.CHOCOLATE.get()) || held.is(ModItems.SODA.get()))) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            this.tame(player);
            this.setOrderedToSit(false);
            this.navigation.stop();
            this.level().broadcastEntityEvent(this, (byte) 7);
            lastQuestOfferTime = this.level().getGameTime();
            player.displayClientMessage(Component.literal("Аля доверилась тебе! Теперь она будет твоим компаньоном."), true);
            return InteractionResult.CONSUME;
        }

        if (!this.isTame()) {
            player.displayClientMessage(Component.literal("Аля пока не знает тебя. Попробуй угостить её печеньем, сладкими ягодами, шоколадом или газировкой."), true);
        }
        return InteractionResult.PASS;
    }
}
