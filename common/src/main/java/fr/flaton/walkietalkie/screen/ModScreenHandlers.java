package fr.flaton.walkietalkie.screen;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import fr.flaton.walkietalkie.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class ModScreenHandlers {
    public static final DeferredRegister<MenuType<?>> SCREEN_HANDLERS = DeferredRegister.create(Constants.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<SpeakerScreenHandler>> SPEAKER = SCREEN_HANDLERS.register("speaker",() -> new MenuType<>(SpeakerScreenHandler::new, FeatureFlags.DEFAULT_FLAGS));

    public static void register() {
        SCREEN_HANDLERS.register();
    }


}
