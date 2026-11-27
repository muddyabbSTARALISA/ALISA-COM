package com.alisa.companion.network;

import com.alisa.companion.AlisaCompanion;
import com.alisa.companion.entity.AlisaEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class AlisaNetwork {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        AlisaCompanion.id("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private AlisaNetwork() { }

    public static void register() {
        CHANNEL.messageBuilder(MenuActionPacket.class, 0)
            .encoder(MenuActionPacket::encode)
            .decoder(MenuActionPacket::decode)
            .consumerMainThread(MenuActionPacket::handle)
            .add();
    }

    public static void sendMenuAction(int entityId, int action) {
        CHANNEL.sendToServer(new MenuActionPacket(entityId, action));
    }

    public record MenuActionPacket(int entityId, int action) {
        static void encode(MenuActionPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.entityId);
            buffer.writeVarInt(packet.action);
        }
        static MenuActionPacket decode(FriendlyByteBuf buffer) {
            return new MenuActionPacket(buffer.readVarInt(), buffer.readVarInt());
        }
        static void handle(MenuActionPacket packet,
                           java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> contextSupplier) {
            net.minecraftforge.network.NetworkEvent.Context context = contextSupplier.get();
            ServerPlayer player = context.getSender();
            if (player == null) {
                context.setPacketHandled(true);
                return;
            }
            Entity entity = player.level().getEntity(packet.entityId);
            if (!(entity instanceof AlisaEntity alisa) || !alisa.isOwnedBy(player)
                || player.distanceToSqr(alisa) > 64.0D) return;
            switch (packet.action) {
                case 0 -> alisa.setOrderedToSit(!alisa.isOrderedToSit());
                case 1 -> alisa.sendRelationshipStatus(player);
                case 2 -> alisa.requestQuestFromMenu(player);
                case 3 -> alisa.sendDialogGreeting(player);
                case 4 -> alisa.toggleStayHere(player);
                default -> { }
            }
            context.setPacketHandled(true);
        }
    }
}
