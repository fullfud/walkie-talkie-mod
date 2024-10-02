package fr.flaton.walkietalkie.network.packet.c2s.walkietalkie;

import dev.architectury.networking.NetworkManager;
import fr.flaton.walkietalkie.Constants;
import fr.flaton.walkietalkie.Util;
import fr.flaton.walkietalkie.item.WalkieTalkieItem;
import fr.flaton.walkietalkie.network.packet.s2c.UpdateWalkieTalkieS2CPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record ButtonWalkieTalkieC2SPacket(int index, boolean status) implements CustomPacketPayload {

    public static final Type<ButtonWalkieTalkieC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "button_walkietalkie_c2s"));
    public static final StreamCodec<ByteBuf, ButtonWalkieTalkieC2SPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, ButtonWalkieTalkieC2SPacket::index, ByteBufCodecs.BOOL, ButtonWalkieTalkieC2SPacket::status, ButtonWalkieTalkieC2SPacket::new);

    public static void receive(ButtonWalkieTalkieC2SPacket type, NetworkManager.PacketContext packetContext) {
        ServerPlayer player = (ServerPlayer) packetContext.getPlayer();

        ItemStack stack = Util.getWalkieTalkieInHand(player);
        if (!(stack.getItem() instanceof WalkieTalkieItem)) {
            return;
        }

        int index = type.index();
        boolean status = type.status();

        switch (index) {
            case 0 -> WalkieTalkieItem.setActivate(stack, status);
            case 1 -> WalkieTalkieItem.setMute(stack, status);
        }

        NetworkManager.sendToPlayer(player, new UpdateWalkieTalkieS2CPacket(stack));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
