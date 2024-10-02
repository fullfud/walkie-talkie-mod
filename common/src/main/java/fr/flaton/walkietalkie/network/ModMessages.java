package fr.flaton.walkietalkie.network;

import dev.architectury.networking.NetworkManager;
import fr.flaton.walkietalkie.network.packet.c2s.*;
import fr.flaton.walkietalkie.network.packet.c2s.speaker.ButtonSpeakerC2SPacket;
import fr.flaton.walkietalkie.network.packet.c2s.speaker.CanalSpeakerC2SPacket;
import fr.flaton.walkietalkie.network.packet.c2s.walkietalkie.ButtonWalkieTalkieC2SPacket;
import fr.flaton.walkietalkie.network.packet.c2s.walkietalkie.CanalWalkieTalkieC2SPacket;
import fr.flaton.walkietalkie.network.packet.s2c.UpdateWalkieTalkieS2CPacket;

public class ModMessages {

    public static void registerC2SPackets() {

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ButtonWalkieTalkieC2SPacket.TYPE, ButtonWalkieTalkieC2SPacket.STREAM_CODEC, ButtonWalkieTalkieC2SPacket::receive);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CanalWalkieTalkieC2SPacket.TYPE, CanalWalkieTalkieC2SPacket.STREAM_CODEC, CanalWalkieTalkieC2SPacket::receive);

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ButtonSpeakerC2SPacket.TYPE, ButtonSpeakerC2SPacket.STREAM_CODEC, ButtonSpeakerC2SPacket::receive);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CanalSpeakerC2SPacket.TYPE, CanalSpeakerC2SPacket.STREAM_CODEC, CanalSpeakerC2SPacket::receive);

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ActivateKeyPressedC2SPacket.TYPE, ActivateKeyPressedC2SPacket.STREAM_CODEC, ActivateKeyPressedC2SPacket::receive);
    }

    public static void registerS2CPackets() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, UpdateWalkieTalkieS2CPacket.TYPE, UpdateWalkieTalkieS2CPacket.STREAM_CODEC, UpdateWalkieTalkieS2CPacket::receive);
    }

}
