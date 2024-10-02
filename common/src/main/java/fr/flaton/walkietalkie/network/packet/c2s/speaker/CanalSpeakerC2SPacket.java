package fr.flaton.walkietalkie.network.packet.c2s.speaker;

import dev.architectury.networking.NetworkManager;
import fr.flaton.walkietalkie.Constants;
import fr.flaton.walkietalkie.config.ModConfig;
import fr.flaton.walkietalkie.screen.SpeakerScreenHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractContainerMenu;

public record CanalSpeakerC2SPacket(int canal) implements CustomPacketPayload {

    public static final Type<CanalSpeakerC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "canal_speaker_c2s"));
    public static final StreamCodec<ByteBuf, CanalSpeakerC2SPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, CanalSpeakerC2SPacket::canal, CanalSpeakerC2SPacket::new);

    public static void receive(CanalSpeakerC2SPacket type, NetworkManager.PacketContext packetContext) {

        ServerPlayer player = (ServerPlayer) packetContext.getPlayer();

        int canal = type.canal();

        AbstractContainerMenu screenHandler = player.containerMenu;

        if (!(screenHandler instanceof SpeakerScreenHandler speakerScreenHandler)) {
            return;
        }

        canal = Mth.clamp(canal, 1, ModConfig.maxCanal);

        speakerScreenHandler.setCanal(canal);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
