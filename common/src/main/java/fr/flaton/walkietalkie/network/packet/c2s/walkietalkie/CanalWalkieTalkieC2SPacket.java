package fr.flaton.walkietalkie.network.packet.c2s.walkietalkie;

import dev.architectury.networking.NetworkManager;
import fr.flaton.walkietalkie.Constants;
import fr.flaton.walkietalkie.Util;
import fr.flaton.walkietalkie.config.ModConfig;
import fr.flaton.walkietalkie.item.WalkieTalkieItem;
import fr.flaton.walkietalkie.network.packet.s2c.UpdateWalkieTalkieS2CPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public record CanalWalkieTalkieC2SPacket(int canal) implements CustomPacketPayload {

    public static final Type<CanalWalkieTalkieC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "canal_walkietalkie_c2s"));
    public static final StreamCodec<ByteBuf, CanalWalkieTalkieC2SPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, CanalWalkieTalkieC2SPacket::canal, CanalWalkieTalkieC2SPacket::new);

    public static void receive(CanalWalkieTalkieC2SPacket type, NetworkManager.PacketContext packetContext) {
        ServerPlayer player = (ServerPlayer) packetContext.getPlayer();

        ItemStack stack = Util.getWalkieTalkieInHand(player);
        if (!(stack.getItem() instanceof WalkieTalkieItem)) {
            return;
        }

        int canal = type.canal();

        WalkieTalkieItem.setCanal(stack, Mth.clamp(canal, 1, ModConfig.maxCanal));

        NetworkManager.sendToPlayer(player, new UpdateWalkieTalkieS2CPacket(stack));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
