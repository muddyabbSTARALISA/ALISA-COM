package com.alisa.companion;

import com.alisa.companion.entity.AlisaEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AlisaCompanion.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class QuestEvents {
    private QuestEvents() { }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;

        BlockState placedState = event.getPlacedBlock();
        var nearbyAlisas = player.level().getEntitiesOfClass(
            AlisaEntity.class,
            new net.minecraft.world.phys.AABB(event.getPos()).inflate(6.0D),
            alisa -> alisa.isOwnedBy(player) && alisa.isRequestedDecorationBlock(placedState.getBlock())
        );

        if (!nearbyAlisas.isEmpty()) {
            nearbyAlisas.get(0).completeDecorationQuest(player);
        }
    }
}
