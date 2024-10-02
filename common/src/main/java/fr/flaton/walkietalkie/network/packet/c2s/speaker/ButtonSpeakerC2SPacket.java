package fr.flaton.walkietalkie.network.packet.c2s.speaker;

import dev.architectury.networking.NetworkManager;
import fr.flaton.walkietalkie.Constants;
import fr.flaton.walkietalkie.screen.SpeakerScreenHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

public record ButtonSpeakerC2SPacket(boolean activate) implements CustomPacketPayload {

    public static final Type<ButtonSpeakerC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "button_speaker_c2s"));
    public static final StreamCodec<ByteBuf, ButtonSpeakerC2SPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, ButtonSpeakerC2SPacket::activate, ButtonSpeakerC2SPacket::new);

    public static void receive(ButtonSpeakerC2SPacket type, NetworkManager.PacketContext packetContext) {

        ServerPlayer player = (ServerPlayer) packetContext.getPlayer();

        boolean activate = type.activate();

        AbstractContainerMenu menu = player.containerMenu;

        if (!(menu instanceof SpeakerScreenHandler speakerScreenHandler)) {
            return;
        }

        speakerScreenHandler.setActivate(activate);


    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
