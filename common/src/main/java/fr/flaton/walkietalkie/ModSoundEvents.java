package fr.flaton.walkietalkie;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSoundEvents {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Constants.MOD_ID, RegistryKeys.SOUND_EVENT);

    // ИЗМЕНЕНО: Новое, уникальное имя для события, чтобы разорвать цикл
    public static final Identifier ON_ID = new Identifier(Constants.MOD_ID, "item.walkietalkie.on");
    public static final RegistrySupplier<SoundEvent> ON_SOUND_EVENT = SOUND_EVENTS.register("item.walkietalkie.on", () -> SoundEvent.of(ON_ID));

    // ИЗМЕНЕНО: Новое, уникальное имя для события, чтобы разорвать цикл
    public static final Identifier OFF_ID = new Identifier(Constants.MOD_ID, "item.walkietalkie.off");
    public static final RegistrySupplier<SoundEvent> OFF_SOUND_EVENT = SOUND_EVENTS.register("item.walkietalkie.off", () -> SoundEvent.of(OFF_ID));

    public static void register() {
        SOUND_EVENTS.register();
    }

}