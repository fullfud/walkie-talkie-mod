package fr.flaton.walkietalkie.neoforge;

import fr.flaton.walkietalkie.Constants;
import fr.flaton.walkietalkie.WalkieTalkie;
import fr.flaton.walkietalkie.client.gui.screen.SpeakerScreen;
import fr.flaton.walkietalkie.config.ModConfig;
import fr.flaton.walkietalkie.screen.ModScreenHandlers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(Constants.MOD_ID)
public class WalkieTalkieNeoForge {
    public WalkieTalkieNeoForge(IEventBus modBus) {
        ModConfig config = new ModConfig(FMLPaths.CONFIGDIR.get());
        config.loadModConfig();

        WalkieTalkie.init();

        if (FMLLoader.getDist() == Dist.CLIENT) {
            modBus.addListener(WalkieTalkieNeoForge::registerScreens);
        }
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModScreenHandlers.SPEAKER.get(), SpeakerScreen::new);
    }
}