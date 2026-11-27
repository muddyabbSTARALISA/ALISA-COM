package com.alisa.companion.client;

import com.alisa.companion.AlisaCompanion;
import com.alisa.companion.entity.AlisaEntity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AlisaCompanion.MOD_ID, value = Dist.CLIENT)
public final class AlisaInputEvents {
    private static int heldTicks;
    private static boolean menuOpened;
    private static int targetId = -1;

    private AlisaInputEvents() { }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) return;
        if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit
            && hit.getEntity() instanceof AlisaEntity alisa && alisa.isOwnedBy(mc.player)) {
            event.setCanceled(true);
            targetId = alisa.getId();
            heldTicks = 0;
            menuOpened = false;
        }
    }

    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (targetId < 0 || mc.player == null || mc.level == null || mc.screen != null) {
            reset();
            return;
        }
        if (net.minecraft.client.Minecraft.ON_OSX) return;
        boolean down = org.lwjgl.glfw.GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), 1)
            == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        if (down) {
            heldTicks++;
            if (heldTicks >= 12 && !menuOpened) {
                var entity = mc.level.getEntity(targetId);
                if (entity instanceof AlisaEntity alisa) {
                    mc.setScreen(new AlisaMenuScreen(alisa));
                    menuOpened = true;
                }
            }
        } else {
            if (!menuOpened && heldTicks < 12 && mc.gameMode != null) {
                var entity = mc.level.getEntity(targetId);
                if (entity instanceof AlisaEntity alisa) {
                    mc.gameMode.interact(mc.player, alisa, net.minecraft.world.InteractionHand.MAIN_HAND);
                }
            }
            reset();
        }
    }

    private static void reset() {
        heldTicks = 0;
        menuOpened = false;
        targetId = -1;
    }
}
