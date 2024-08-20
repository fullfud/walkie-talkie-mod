package fr.flaton.walkietalkie.network;

import dev.architectury.networking.NetworkManager;
import fr.flaton.walkietalkie.Constants;
import fr.flaton.walkietalkie.network.packet.c2s.*;
import fr.flaton.walkietalkie.network.packet.c2s.speaker.ButtonSpeakerC2SPacket;
import fr.flaton.walkietalkie.network.packet.c2s.speaker.CanalSpeakerC2SPacket;
import fr.flaton.walkietalkie.network.packet.c2s.walkietalkie.ButtonWalkieTalkieC2SPacket;
import fr.flaton.walkietalkie.network.packet.c2s.walkietalkie.CanalWalkieTalkieC2SPacket;
import fr.flaton.walkietalkie.network.packet.s2c.UpdateWalkieTalkieS2CPacket;
import net.minecraft.util.Identifier;

public class ModMessages {
    public static final Identifier UPDATE_WALKIETALKIE_S2C = new Identifier(Constants.MOD_ID, "updatewalkietalkie_s2c");

    public static final Identifier BUTTON_WALKIETALKIE_C2S = new Identifier(Constants.MOD_ID, "buttonwalkietalkie_c2s");
    public static final Identifier CANAL_WALKIETALKIE_C2S = new Identifier(Constants.MOD_ID, "canalwalkietalkie_c2s");

    public static final Identifier BUTTON_SPEAKER_C2S = new Identifier(Constants.MOD_ID, "button_speaker_c2s");
    public static final Identifier CANAL_SPEAKER_C2S = new Identifier(Constants.MOD_ID, "canal_speaker_c2s");

    public static final Identifier ACTIVATE_KEY_PRESSED_C2S = new Identifier(Constants.MOD_ID, "activatekeypressed_c2s");

    public static void registerC2SPackets() {

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, BUTTON_WALKIETALKIE_C2S, ButtonWalkieTalkieC2SPacket::receive);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CANAL_WALKIETALKIE_C2S, CanalWalkieTalkieC2SPacket::receive);

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, BUTTON_SPEAKER_C2S, ButtonSpeakerC2SPacket::receive);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CANAL_SPEAKER_C2S, CanalSpeakerC2SPacket::receive);

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ACTIVATE_KEY_PRESSED_C2S, ActivateKeyPressedC2SPacket::receive);
    }

    public static void registerS2CPackets() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, UPDATE_WALKIETALKIE_S2C, UpdateWalkieTalkieS2CPacket::receive);
    }

}
