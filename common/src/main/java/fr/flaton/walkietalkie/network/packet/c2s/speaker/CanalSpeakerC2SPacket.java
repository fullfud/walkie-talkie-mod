package fr.flaton.walkietalkie.network.packet.c2s.speaker;

import dev.architectury.networking.NetworkManager;
import fr.flaton.walkietalkie.config.ModConfig;
import fr.flaton.walkietalkie.screen.SpeakerScreenHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;

public class CanalSpeakerC2SPacket {

    public static void receive(PacketByteBuf packetByteBuf, NetworkManager.PacketContext packetContext) {

        ServerPlayerEntity player = (ServerPlayerEntity) packetContext.getPlayer();

        int canal = packetByteBuf.readInt();

        ScreenHandler screenHandler = player.currentScreenHandler;

        if (!(screenHandler instanceof SpeakerScreenHandler speakerScreenHandler)) {
            return;
        }

        canal = MathHelper.clamp(canal, 1, ModConfig.maxCanal);

        speakerScreenHandler.setCanal(canal);
    }
}
