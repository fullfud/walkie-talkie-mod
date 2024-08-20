package fr.flaton.walkietalkie.network.packet.c2s.speaker;

import dev.architectury.networking.NetworkManager;
import fr.flaton.walkietalkie.screen.SpeakerScreenHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public class ButtonSpeakerC2SPacket {
    public static void receive(PacketByteBuf packetByteBuf, NetworkManager.PacketContext packetContext) {

        ServerPlayerEntity player = (ServerPlayerEntity) packetContext.getPlayer();

        boolean activate = packetByteBuf.readBoolean();

        ScreenHandler screenHandler = player.currentScreenHandler;

        if (!(screenHandler instanceof SpeakerScreenHandler speakerScreenHandler)) {
            return;
        }

        speakerScreenHandler.setActivate(activate);


    }
}
