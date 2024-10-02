package fr.flaton.walkietalkie.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import fr.flaton.walkietalkie.network.packet.c2s.ActivateKeyPressedC2SPacket;
import net.minecraft.client.KeyMapping;

public class KeyBindings {

    public static final KeyMapping ACTIVATE = new KeyMapping(
            "key.walkietalkie.activate",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_B,
            "category.walkietalkie.keys"
    );

    public static void register() {
        KeyMappingRegistry.register(ACTIVATE);

        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            while (ACTIVATE.consumeClick()) {
                NetworkManager.sendToServer(ActivateKeyPressedC2SPacket.INSTANCE);
            }
        });
    }

}
