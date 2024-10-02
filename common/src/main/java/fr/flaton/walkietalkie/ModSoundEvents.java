package fr.flaton.walkietalkie;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class ModSoundEvents {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Constants.MOD_ID, Registries.SOUND_EVENT);

    public static final ResourceLocation ON_ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "walkietalkie_on");
    public static final RegistrySupplier<SoundEvent> ON_SOUND_EVENT = SOUND_EVENTS.register("walkietalkie_on", () -> SoundEvent.createVariableRangeEvent(ON_ID));

    public static final ResourceLocation OFF_ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "walkietalkie_off");
    public static final RegistrySupplier<SoundEvent> OFF_SOUND_EVENT = SOUND_EVENTS.register("walkietalkie_off", () -> SoundEvent.createVariableRangeEvent(OFF_ID));

    public static void register() {
        SOUND_EVENTS.register();
    }

}
